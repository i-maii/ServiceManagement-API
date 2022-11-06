package com.example.servicemanagement.service;

import com.example.servicemanagement.entity.Config;
import com.example.servicemanagement.entity.Request;
import com.example.servicemanagement.repository.ConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.example.servicemanagement.constant.Constant.*;

@Service
public class ConfigService {

    private static Logger logger = LoggerFactory.getLogger(ConfigService.class);

    @Autowired
    ConfigRepository configRepository;

    @Autowired
    RequestService requestService;

    public void findConfiguration() {
        logger.info("---- เริ่มคำนวณหาจำนวนชั่วโมงและจำนวนช่าง ----");
        List<Request> allRequest = this.requestService.getRequestByStatus(STATUS_READY_FOR_PLAN);
        boolean require2Technician = this.requestService.checkRequire2Technician(allRequest);

        int totalRequestHour = this.requestService.getTotalRequestHour(allRequest, require2Technician);
        int totalPriorityRequestHour = this.requestService.getTotalPriorityHour(allRequest, require2Technician);
        int lowestTotalPriorityHour = this.requestService.getLowestTotalPriorityHour();
        int lowestTotalRequestHour = this.requestService.getLowestTotalRequestHour();

        int usageTechnician = 1;
        int totalTargetHour = 8;
        Integer[] targetHour = new Integer[1];

        updateConfigByKey(KEY_TOTAL_REQUEST_HOUR, String.valueOf(totalRequestHour));
        updateConfigByKey(KEY_TOTAL_PRIORITY_HOUR, String.valueOf(totalPriorityRequestHour));
        logger.info("จำนวนชั่วโมงรวมของรายการแจ้งซ่อมทั้งหมด: {} ชั่วโมง", totalRequestHour);
        logger.info("จำนวนชั่วโมงรวมของงานที่มีลำดับความสำคัญลำดับที่ 1 และ 2: {} ชั่วโมง", totalPriorityRequestHour);

        if (totalRequestHour > 16 && totalRequestHour <= 20) {
            if (totalPriorityRequestHour > 16) {
                usageTechnician = 3;
                targetHour = new Integer[3];
            } else {
                usageTechnician = 2;
                targetHour = new Integer[2];
            }
        } else if (totalRequestHour > 20) {
            usageTechnician = 3;
            targetHour = new Integer[3];
        } else if (totalRequestHour > 8) {
            usageTechnician = 2;
            targetHour = new Integer[2];
        }

        updateConfigByKey(KEY_USAGE_TECHNICIAN, String.valueOf(usageTechnician));
        logger.info("จำนวนช่างซ่อมที่ต้องใช้: {} คน", usageTechnician);

        int priorityHourMin = 0;
        int priorityHourMax = 0;
        int lowestPriorityHourMin = 0;
        int lowestPriorityHourMax = 0;

        if (usageTechnician == 1) {
            if (totalRequestHour < 8) {
                totalTargetHour = totalRequestHour;
                targetHour[0] = totalRequestHour;
            } else {
                targetHour[0] = 8;
            }

            priorityHourMax = Math.min(totalPriorityRequestHour, 8);
        } else if (usageTechnician == 2) {
            if (totalRequestHour < 16) {
                totalTargetHour = totalRequestHour;
                targetHour[0] = totalRequestHour / 2;
                targetHour[1] = totalRequestHour - targetHour[0];
            } else {
                totalTargetHour = 16;
                targetHour[0] = 8;
                targetHour[1] = 8;
            }

            if (totalPriorityRequestHour >= 16) {
                priorityHourMax = 8;
            } else {
                priorityHourMin = totalPriorityRequestHour / 2;
                priorityHourMax =  Math.min(totalPriorityRequestHour, 8);
            }
        } else {
            if (totalRequestHour < 24) {
                totalTargetHour = totalRequestHour;
                targetHour[0] = totalRequestHour / 3;
                targetHour[1] = (totalRequestHour - targetHour[0]) / 2;
                targetHour[2] = totalRequestHour - targetHour[1];
            } else {
                if (lowestTotalRequestHour < 8) {
                    totalTargetHour = 16 + lowestTotalRequestHour;
                    targetHour[0] = 8;
                    targetHour[1] = 8;
                    targetHour[2] = lowestTotalRequestHour;
                } else {
                    totalTargetHour = 24;
                    targetHour[0] = 8;
                    targetHour[1] = 8;
                    targetHour[2] = 8;
                }
            }

            int avgPriorityHour = totalPriorityRequestHour / 3;

            if (lowestTotalPriorityHour < avgPriorityHour) {
                lowestPriorityHourMax = lowestTotalPriorityHour;

                int remainingPriorityHour = totalPriorityRequestHour - lowestTotalPriorityHour;
                if (remainingPriorityHour > 16) {
                    priorityHourMax = 8;
                } else {
                    priorityHourMin = remainingPriorityHour / 2;
                    priorityHourMax =  Math.min(totalPriorityRequestHour, 8);
                }
            } else {
                lowestPriorityHourMin = avgPriorityHour;
                lowestPriorityHourMax = Math.min(lowestTotalPriorityHour, 8);
                priorityHourMin = avgPriorityHour;
                priorityHourMax =  Math.min(totalPriorityRequestHour, 8);
            }
        }

        updateConfigByKey(KEY_TOTAL_TARGET_HOUR, String.valueOf(totalTargetHour));
        logger.info("จำนวนชั่วโมงรวมของงานที่ต้องทำ: {} ชั่วโมง", totalTargetHour);

        String targetHour1 = String.valueOf(targetHour[0]);
        String targetHour2 = "0";
        String targetHour3 = "0";
        String technician3TotalRequestHour = "0";
        String technician3TotalPriorityHour = "0";
        if (targetHour.length == 2) {
            targetHour2 = String.valueOf(targetHour[1]);
        } else if (targetHour.length == 3) {
            targetHour2 = String.valueOf(targetHour[1]);
            targetHour3 = String.valueOf(targetHour[2]);
            technician3TotalRequestHour = String.valueOf(lowestTotalRequestHour);
            technician3TotalPriorityHour = String.valueOf(lowestTotalRequestHour);
        }

        updateConfigByKey(KEY_TECHNICIAN_TARGET_HOUR_1, targetHour1);
        updateConfigByKey(KEY_TECHNICIAN_TARGET_HOUR_2, targetHour2);
        updateConfigByKey(KEY_TECHNICIAN_TARGET_HOUR_3, targetHour3);
        logger.info("จำนวนชั่วโมงของงานที่ช่างคนที่ 1 ต้องทำ: {} ชั่วโมง", targetHour1);
        logger.info("จำนวนชั่วโมงของงานที่ช่างคนที่ 2 ต้องทำ: {} ชั่วโมง", targetHour2);
        logger.info("จำนวนชั่วโมงของงานที่ช่างคนที่ 3 ต้องทำ: {} ชั่วโมง", targetHour3);

        updateConfigByKey(KEY_LOWEST_TOTAL_PRIORITY_HOUR, technician3TotalPriorityHour);
        updateConfigByKey(KEY_LOWEST_TOTAL_REQUEST_HOUR, technician3TotalRequestHour);
        logger.info("จำนวนชั่วโมงรวมของงานที่มีลำดับความสำคัญลำดับที่ 1 และ 2 ที่ตรงกับความสามารถของช่างคนที่ 3: {} ชั่วโมง", technician3TotalPriorityHour);
        logger.info("จำนวนชั่วโมงรวมของรายการแจ้งซ่อมทั้งหมด ที่ตรงกับความสามารถของช่างคนที่ 3: {} ชั่วโมง", technician3TotalRequestHour);

        updateConfigByKey(KEY_PRIORITY_HOUR_MIN, String.valueOf(priorityHourMin));
        updateConfigByKey(KEY_PRIORITY_HOUR_MAX, String.valueOf(priorityHourMax));
        updateConfigByKey(KEY_LOWEST_PRIORITY_HOUR_MIN, String.valueOf(lowestPriorityHourMin));
        updateConfigByKey(KEY_LOWEST_PRIORITY_HOUR_MAX, String.valueOf(lowestPriorityHourMax));
        logger.info("จำนวนชั่วโมงขั้นต่ำของงานที่มีลำดับความสำคัญลำดับที่ 1 และ 2 ของช่างคนที่ 1 และ 2 ต้องทำ: {} ชั่วโมง", priorityHourMin);
        logger.info("จำนวนชั่วโมงมากที่สุดของงานที่มีลำดับความสำคัญลำดับที่ 1 และ 2 ของช่างคนที่ 1 และ 2 สามารถทำได้: {} ชั่วโมง", priorityHourMax);
        logger.info("จำนวนชั่วโมงขั้นต่ำของงานที่มีลำดับความสำคัญลำดับที่ 1 และ 2 ของช่างคนที่ 3 ต้องทำ: {} ชั่วโมง", lowestPriorityHourMin);
        logger.info("จำนวนชั่วโมงมากที่สุดของงานที่มีลำดับความสำคัญลำดับที่ 1 และ 2 ของช่างคนที่ 3 สามารถทำได้: {} ชั่วโมง", lowestPriorityHourMax);

        logger.info("-----------------------------------------\n");
    }

    public void updateDrive(int technicianId) {
        updateConfigByKey(KEY_DRIVER, String.valueOf(technicianId));
    }

    private void updateConfigByKey(String key, String value) {
        Config config = this.configRepository.findConfigByKey(key);
        config.setValue(value);
        this.configRepository.saveAndFlush(config);
    }

    public Integer getTechnician1TargetHourConfig() {
        Config config = this.configRepository.findConfigByKey(KEY_TECHNICIAN_TARGET_HOUR_1);
        return Integer.parseInt(config.getValue());
    }

    public Integer getTechnician2TargetHourConfig() {
        Config config = this.configRepository.findConfigByKey(KEY_TECHNICIAN_TARGET_HOUR_2);
        return Integer.parseInt(config.getValue());
    }

    public Integer getTechnician3TargetHourConfig() {
        Config config = this.configRepository.findConfigByKey(KEY_TECHNICIAN_TARGET_HOUR_3);
        return Integer.parseInt(config.getValue());
    }

    public Integer getUsageTechnicianConfig() {
       Config config = this.configRepository.findConfigByKey(KEY_USAGE_TECHNICIAN);
        return Integer.parseInt(config.getValue());
    }

    public Integer[] getAllTargetHour() {
        return new Integer[]{getTechnician1TargetHourConfig(), getTechnician2TargetHourConfig(), getTechnician3TargetHourConfig()};
    }

    public Integer getPriorityHourMin() {
        Config config = this.configRepository.findConfigByKey(KEY_PRIORITY_HOUR_MIN);
        return Integer.parseInt(config.getValue());
    }

    public Integer getPriorityHourMax() {
        Config config = this.configRepository.findConfigByKey(KEY_PRIORITY_HOUR_MAX);
        return Integer.parseInt(config.getValue());
    }

    public Integer[] getRangePriorityHour() {
        return new Integer[]{getPriorityHourMin(), getPriorityHourMax()};
    }

    public Integer getLowestPriorityHourMin() {
        Config config = this.configRepository.findConfigByKey(KEY_LOWEST_PRIORITY_HOUR_MIN);
        return Integer.parseInt(config.getValue());
    }

    public Integer getLowestPriorityHourMax() {
        Config config = this.configRepository.findConfigByKey(KEY_LOWEST_PRIORITY_HOUR_MAX);
        return Integer.parseInt(config.getValue());
    }

    public Integer[] getRangeLowestPriorityHour() {
        return new Integer[]{getLowestPriorityHourMin(), getLowestPriorityHourMax()};
    }

    public boolean checkTotalHourMoreThanTargetHour() {
        int totalHour = getConfigByKey(KEY_TOTAL_REQUEST_HOUR);
        int targetHour = getConfigByKey(KEY_TOTAL_TARGET_HOUR);

        return totalHour > targetHour;
    }

    public int getTotalTargetHour() {
        Config config = this.configRepository.findConfigByKey(KEY_TOTAL_TARGET_HOUR);
        return Integer.parseInt(config.getValue());
    }

    public int getTotalRequestHour() {
        Config config = this.configRepository.findConfigByKey(KEY_TOTAL_REQUEST_HOUR);
        return Integer.parseInt(config.getValue());
    }

    public int getTotalPriorityHour() {
        Config config = this.configRepository.findConfigByKey(KEY_TOTAL_PRIORITY_HOUR);
        return Integer.parseInt(config.getValue());
    }

    public int getTotalLowestPriorityHour() {
        Config config = this.configRepository.findConfigByKey(KEY_LOWEST_TOTAL_PRIORITY_HOUR);
        return Integer.parseInt(config.getValue());
    }

    public int getTotalLowestRequestHour() {
        Config config = this.configRepository.findConfigByKey(KEY_LOWEST_TOTAL_REQUEST_HOUR);
        return Integer.parseInt(config.getValue());
    }

    public int getConfigByKey(String key) {
        Config config = this.configRepository.findConfigByKey(key);
        return Integer.parseInt(config.getValue());
    }

    public void updatePriorityHour (int priorityHour) {
        int avgPriorityHour = priorityHour / 2;

        updateConfigByKey(KEY_TOTAL_PRIORITY_HOUR, String.valueOf(priorityHour));
        updateConfigByKey(KEY_PRIORITY_HOUR_MIN, String.valueOf(avgPriorityHour));
    }
}

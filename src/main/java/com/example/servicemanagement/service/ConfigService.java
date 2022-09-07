package com.example.servicemanagement.service;

import com.example.servicemanagement.entity.Config;
import com.example.servicemanagement.entity.Request;
import com.example.servicemanagement.repository.ConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.example.servicemanagement.constant.Constant.*;

@Service
public class ConfigService {

    @Autowired
    ConfigRepository configRepository;

    @Autowired
    RequestService requestService;

    public void findConfiguration() {
        int totalRequestHour = this.requestService.getTotalRequestHour();
        int totalPriorityRequestHour = this.requestService.getTotalPriorityHour();
        int lowestTotalPriorityHour = this.requestService.getLowestTotalPriorityHour();

        int usageTechnician = 1;
        int totalTargetHour = 8;
        Integer[] targetHour = new Integer[1];

        updateConfigByKey(KEY_TOTAL_REQUEST_HOUR, String.valueOf(totalRequestHour));
        updateConfigByKey(KEY_TOTAL_PRIORITY_HOUR, String.valueOf(totalPriorityRequestHour));
        updateConfigByKey(KEY_LOWEST_TOTAL_PRIORITY_HOUR, String.valueOf(lowestTotalPriorityHour));

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

            priorityHourMin = Math.min(totalPriorityRequestHour, 8);
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
                priorityHourMin = 8;
            } else {
                priorityHourMin = totalPriorityRequestHour / 2;
                priorityHourMax = 8;
            }
        } else {
            if (totalRequestHour < 24) {
                totalTargetHour = totalRequestHour;
                targetHour[0] = totalRequestHour / 3;
                targetHour[1] = (totalRequestHour - targetHour[0]) / 2;
                targetHour[2] = totalRequestHour - targetHour[1];
            } else {
                int lowestTotalHour = this.requestService.getLowestTotalHour();
                if (lowestTotalHour < 8) {
                    totalTargetHour = 16 + lowestTotalHour;
                    targetHour[0] = 8;
                    targetHour[1] = 8;
                    targetHour[2] = lowestTotalHour;
                } else {
                    totalTargetHour = 24;
                    targetHour[0] = 8;
                    targetHour[1] = 8;
                    targetHour[2] = 8;
                }
            }

            int avgPriorityHour = totalPriorityRequestHour / 3;

            if (lowestTotalPriorityHour < avgPriorityHour) {
                lowestPriorityHourMin = lowestTotalPriorityHour;

                int remainingPriorityHour = totalPriorityRequestHour - lowestTotalPriorityHour;
                if (remainingPriorityHour > 16) {
                    priorityHourMin = 8;
                } else {
                    priorityHourMin = remainingPriorityHour / 2;
                    priorityHourMax = 8;
                }
            } else {
                lowestPriorityHourMin = avgPriorityHour;
                lowestPriorityHourMax = 8;
                priorityHourMin = avgPriorityHour;
                priorityHourMax = 8;
            }
        }

        updateConfigByKey(KEY_TOTAL_TARGET_HOUR, String.valueOf(totalTargetHour));

        updateConfigByKey(KEY_TECHNICIAN_TARGET_HOUR_1, String.valueOf(targetHour[0]));
        if (targetHour.length == 2) {
            updateConfigByKey(KEY_TECHNICIAN_TARGET_HOUR_2, String.valueOf(targetHour[1]));
        }
        if (targetHour.length == 3) {
            updateConfigByKey(KEY_TECHNICIAN_TARGET_HOUR_3, String.valueOf(targetHour[2]));
        }

        updateConfigByKey(KEY_PRIORITY_HOUR_MIN, String.valueOf(priorityHourMin));
        updateConfigByKey(KEY_PRIORITY_HOUR_MAX, String.valueOf(priorityHourMax));
        updateConfigByKey(KEY_LOWEST_PRIORITY_HOUR_MIN, String.valueOf(lowestPriorityHourMin));
        updateConfigByKey(KEY_LOWEST_PRIORITY_HOUR_MAX, String.valueOf(lowestPriorityHourMax));

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

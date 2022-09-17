package com.example.servicemanagement.service;

import com.example.servicemanagement.dto.TechnicianPlanDto;
import com.example.servicemanagement.entity.Request;
import com.example.servicemanagement.entity.Schedule;
import com.example.servicemanagement.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import static com.example.servicemanagement.constant.Constant.*;

@Service
public class ScheduleService {

    private int totalApartment = 6;
    private long totalDate = Long.MAX_VALUE;
    private boolean keepworking = true;
    private int bestRequestPlanId = 0;
    private int totalPriority = Integer.MAX_VALUE;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    RequestService requestService;

    @Autowired
    TechnicianService technicianService;

    @Autowired
    ConfigService configService;

    @Autowired
    RouteService routeService;

    public void findRequestWithSpecificHour() throws ParseException {
        this.configService.findConfiguration();

        Integer[] rangePriorityHour = this.configService.getRangePriorityHour();
        Integer[] lowestRange = this.configService.getRangeLowestPriorityHour();

        Integer usageTechnician = this.configService.getUsageTechnicianConfig();
        Integer[] targetHour = this.configService.getAllTargetHour();

        if (usageTechnician != 0) {
            List<Request> allRequest = this.requestService.getAllRequestForPlanning();
            boolean haveOlderRequest = this.requestService.checkOlderRequest(allRequest);
            boolean isRequire2Technician = this.requestService.checkRequire2Technician(allRequest);

            List<TechnicianPlanDto> requestListForPlan;
            List<TechnicianPlanDto> priorityRequestList;
            if (haveOlderRequest) {
                requestListForPlan = this.requestService.reorderPriority(allRequest);
                priorityRequestList = requestListForPlan.stream().filter(req -> ALL_PRIORITY.contains(req.getPriority())).toList();
            } else {
                requestListForPlan = this.requestService.requestListToTechnicianPlan(allRequest);
                priorityRequestList = requestListForPlan.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority())).toList();
            }

            if (isRequire2Technician) {
                if (usageTechnician == 3) {
                    findRequire2TechnicianPlanFor3Technician(requestListForPlan, lowestRange, targetHour);
                } else {
                    List<TechnicianPlanDto> require2RequestList = findRequire2RequestList(targetHour, requestListForPlan);
                    findRequire2TechnicianPlanFor2Technician(require2RequestList, targetHour);
                }
            } else {
                if (usageTechnician == 3) { //ใช้ช่าง 3 คน
                    findTechnicianPlanFor3Technician(requestListForPlan, lowestRange, targetHour);
                } else {
                    int totalTargetHour = this.configService.getTotalTargetHour();
                    int totalRequestHour = this.configService.getTotalRequestHour();
                    int totalPriorityHour = this.configService.getTotalPriorityHour();

                    List<TechnicianPlanDto> requestList = findRequestList(totalPriorityHour, totalTargetHour, totalRequestHour, requestListForPlan, priorityRequestList);

                    if (usageTechnician == 1) {
                        List<List<TechnicianPlanDto>> planForTechnician1 = new ArrayList<>();
                        planForTechnician1.add(requestList);
                        saveTechnicianPlan(planForTechnician1);
                    } else {
                        findTechnicianPlanFor2Technician(targetHour, rangePriorityHour, requestList);
                    }
                }
            }
        }
    }

    private List<List<TechnicianPlanDto>> findTechnicianPlanForLowestTechnician(Integer[] targetHour, List<TechnicianPlanDto> allRequest, Integer[] lowestRangePriority) {
        List<TechnicianPlanDto> lowestTechnicianRequest = this.requestService.getLowestRequest(allRequest).stream().sorted(Comparator.comparingInt(TechnicianPlanDto::getEstimateTime)).toList();
        int totalLowestRequestHour = this.configService.getTotalLowestRequestHour();

        List<List<TechnicianPlanDto>> possibleLowestPlanList = new ArrayList<>();
        if (totalLowestRequestHour > targetHour[2]) {
            totalApartment = 6;
            List<TechnicianPlanDto> possibleLowestPlan = new ArrayList<>();
            findPossibleRequest(possibleLowestPlanList, possibleLowestPlan, targetHour[2], lowestTechnicianRequest, 0, lowestRangePriority);
        } else {
            possibleLowestPlanList.add(lowestTechnicianRequest);
        }

        return possibleLowestPlanList;
    }

    private void findTechnicianPlanFor2Technician(Integer[] targetHour, Integer[] range, List<TechnicianPlanDto> requestList) {
        List<TechnicianPlanDto> sortedRequestList = requestList.stream().sorted(Comparator.comparingInt(TechnicianPlanDto::getEstimateTime)).toList();

        totalApartment = 6;
        List<List<TechnicianPlanDto>> possiblePlanListForTechnician1 = new ArrayList<>();
        List<TechnicianPlanDto> possiblePlanForTechnician1 = new ArrayList<>();
        findPossibleRequest(possiblePlanListForTechnician1, possiblePlanForTechnician1, targetHour[0], sortedRequestList, 0, range);

        if (possiblePlanListForTechnician1.size() > 1) {
            List<List<TechnicianPlanDto>> possiblePlanListForTechnician2 = new ArrayList<>();
            List<List<TechnicianPlanDto>> tempPossiblePlanListForTechnician1 = new ArrayList<>();
            totalApartment = 6;

            for (List<TechnicianPlanDto> plan : possiblePlanListForTechnician1) {
                List<TechnicianPlanDto> possiblePlanForTechnician2 = new ArrayList<>(requestList.stream().filter(Predicate.not(plan::contains)).toList());
                int numOfApartment = this.findNumberOfApartment(possiblePlanForTechnician2);
                if (numOfApartment < totalApartment) {
                    totalApartment = numOfApartment;
                    tempPossiblePlanListForTechnician1.clear();
                    possiblePlanListForTechnician2.clear();
                    tempPossiblePlanListForTechnician1.add(plan);
                    possiblePlanListForTechnician2.add(possiblePlanForTechnician2);
                } else if (numOfApartment == totalApartment) {
                    tempPossiblePlanListForTechnician1.add(plan);
                    possiblePlanListForTechnician2.add(possiblePlanForTechnician2);
                }
            }
            this.routeService.checkBestRoute(tempPossiblePlanListForTechnician1, possiblePlanListForTechnician2);

            // TODO: save best plan
        } else {
            List<TechnicianPlanDto> planForTechnician2 = requestList.stream().filter(Predicate.not(possiblePlanListForTechnician1.get(0)::contains)).toList();
            possiblePlanListForTechnician1.add(planForTechnician2);

            saveTechnicianPlan(possiblePlanListForTechnician1);
        }
    }

    private void findTechnicianPlanFor3Technician(List<TechnicianPlanDto> allRequest, Integer[] lowestRangePriority, Integer[] targetHour) {
        List<List<TechnicianPlanDto>> possibleLowestPlanList = findTechnicianPlanForLowestTechnician(targetHour, allRequest, lowestRangePriority);

        List<List<TechnicianPlanDto>> otherRequestList = new ArrayList<>();
        if (possibleLowestPlanList.size() > 1) {
            for (List<TechnicianPlanDto> lowestPlan: possibleLowestPlanList) {
                List<TechnicianPlanDto> possibleOtherPlanList = findOtherRequestList(allRequest, lowestPlan, targetHour);
                otherRequestList.add(possibleOtherPlanList);
            }
        } else {
            List<TechnicianPlanDto> requestList = findOtherRequestList(allRequest, possibleLowestPlanList.get(0), targetHour);
            otherRequestList.add(requestList);
        }

        List<TechnicianPlanDto> bestRequest = checkBestRequest(otherRequestList).stream().sorted(Comparator.comparingInt(TechnicianPlanDto::getEstimateTime)).toList();

        //save plan for lowestTechnician
        List<TechnicianPlanDto> lowestTechnicianPlan = possibleLowestPlanList.get(bestRequestPlanId);
        saveLowestTechnicianPlan(lowestTechnicianPlan);

        Integer[] rangePriority = this.configService.getRangePriorityHour();

        findTechnicianPlanFor2Technician(targetHour, rangePriority, bestRequest);

//        totalApartment = 6;
//        List<List<TechnicianPlanDto>> possiblePlanListForTechnician1 = new ArrayList<>();
//        List<TechnicianPlanDto> possiblePlan = new ArrayList<>();
//        findPossibleRequest(possiblePlanListForTechnician1, possiblePlan, targetHour[0], bestRequest, 0, rangePriority);
//
//        if (possiblePlan.size() > 1) {
//            List<List<TechnicianPlanDto>> possiblePlanListForTechnician2 = new ArrayList<>();
//            for (List<TechnicianPlanDto> plan: possiblePlanListForTechnician1) {
//                List<TechnicianPlanDto> possiblePlanForTechnician2 = new ArrayList<>(bestRequest.stream().filter(Predicate.not(plan::contains)).toList());
//                possiblePlanListForTechnician2.add(possiblePlanForTechnician2);
//            }
//            this.routeService.checkBestRoute(possiblePlanListForTechnician1, possiblePlanListForTechnician2);
//            // TODO: save best plan
//        } else {
//            List<TechnicianPlanDto> planForTechnician2 = bestRequest.stream().filter(Predicate.not(possiblePlanListForTechnician1.get(0)::contains)).toList();
//            possiblePlanListForTechnician1.add(planForTechnician2);
//            saveTechnicianPlan(possiblePlanListForTechnician1);
//        }
    }

    private void findRequire2TechnicianPlanFor2Technician(List<TechnicianPlanDto> allRequest, Integer[] targetHour) {
        List<TechnicianPlanDto> requestList = allRequest.stream().filter(req -> req.getRequest().getEstimateTechnician() == 1).toList();
        List<TechnicianPlanDto> require2RequestList = allRequest.stream().filter(Predicate.not(requestList::contains)).toList();
        List<TechnicianPlanDto> sortedRequestList = requestList.stream().sorted(Comparator.comparingInt(TechnicianPlanDto::getEstimateTime)).toList();

        int target = targetHour[0] - require2RequestList.stream().mapToInt(TechnicianPlanDto::getEstimateTime).sum();
        List<List<TechnicianPlanDto>> possiblePlanListForTechnician1 = new ArrayList<>();
        List<TechnicianPlanDto> possiblePlanForTechnician1 = new ArrayList<>();
        findPossibleOtherRequire2Request(possiblePlanListForTechnician1, possiblePlanForTechnician1, target, sortedRequestList, 0);

        if (possiblePlanListForTechnician1.size() > 1) {
            List<List<TechnicianPlanDto>> possiblePlanListForTechnician2 = new ArrayList<>();
            List<List<TechnicianPlanDto>> tempPossiblePlanListForTechnician1 = new ArrayList<>();
            totalApartment = 6;

            for (List<TechnicianPlanDto> plan: possiblePlanListForTechnician1) {
                List<TechnicianPlanDto> planForTech1 = new ArrayList<>(require2RequestList);
                planForTech1.addAll(plan);

                List<TechnicianPlanDto> planForTech2 = new ArrayList<>(require2RequestList);
                planForTech2.addAll(requestList.stream().filter(Predicate.not(plan::contains)).toList());

                int numOfApartment = this.findNumberOfApartment(planForTech2);
                if (numOfApartment < totalApartment) {
                    totalApartment = numOfApartment;
                    tempPossiblePlanListForTechnician1.clear();
                    possiblePlanListForTechnician2.clear();
                    tempPossiblePlanListForTechnician1.add(planForTech1);
                    possiblePlanListForTechnician2.add(planForTech2);
                } else if (numOfApartment == totalApartment) {
                    tempPossiblePlanListForTechnician1.add(planForTech1);
                    possiblePlanListForTechnician2.add(planForTech2);
                }
            }

            if (tempPossiblePlanListForTechnician1.size() > 1) {
                this.routeService.checkBestRoute(tempPossiblePlanListForTechnician1, possiblePlanListForTechnician2);
            }

            List<List<TechnicianPlanDto>> planList = new ArrayList<>();
            planList.add(tempPossiblePlanListForTechnician1.get(0));
            planList.add(possiblePlanListForTechnician2.get(0));

            saveTechnicianPlan(planList);
        } else {
            List<TechnicianPlanDto> planForTechnician1 = new ArrayList<>(require2RequestList);
            planForTechnician1.addAll(possiblePlanListForTechnician1.get(0));

            List<TechnicianPlanDto> planForTechnician2 = requestList.stream().filter(Predicate.not(possiblePlanListForTechnician1.get(0)::contains)).toList();
            planForTechnician2.addAll(require2RequestList);

            List<List<TechnicianPlanDto>> planList = new ArrayList<>();
            planList.add(planForTechnician1);
            planList.add(planForTechnician2);

            saveTechnicianPlan(planList);
        }
    }

    private void findRequire2TechnicianPlanFor3Technician(List<TechnicianPlanDto> requestListForPlan, Integer[] lowestRange, Integer[] targetHour) {
        List<List<TechnicianPlanDto>> possibleLowestPlanList = findTechnicianPlanForLowestTechnician(targetHour, requestListForPlan, lowestRange);

        List<List<TechnicianPlanDto>> otherRequestList = new ArrayList<>();
        if (possibleLowestPlanList.size() > 1) {
            for (List<TechnicianPlanDto> possibleLowestPlan: possibleLowestPlanList) {
                List<TechnicianPlanDto> possibleOtherPlanList = findOtherRequire2RequestList(requestListForPlan, possibleLowestPlan, targetHour);
                otherRequestList.add(possibleOtherPlanList);
            }
        } else {
            List<TechnicianPlanDto> possibleOtherPlanList = findOtherRequire2RequestList(requestListForPlan, possibleLowestPlanList.get(0), targetHour);
            otherRequestList.add(possibleOtherPlanList);
        }

        List<TechnicianPlanDto> bestRequest = checkBestRequest(otherRequestList).stream().sorted(Comparator.comparingInt(TechnicianPlanDto::getEstimateTime)).toList();

        List<TechnicianPlanDto> lowestTechnicianPlan = possibleLowestPlanList.get(bestRequestPlanId);
        saveLowestTechnicianPlan(lowestTechnicianPlan);

        findRequire2TechnicianPlanFor2Technician(bestRequest, targetHour);
    }

    private void saveTechnicianPlan(List<List<TechnicianPlanDto>> technicianPlanList) {
        int index = 1;
        for (List<TechnicianPlanDto> planList: technicianPlanList) {
            for (TechnicianPlanDto plan: planList) {
                Schedule schedule = new Schedule();
                schedule.setRequest(plan.getRequest());
                schedule.setApartment(plan.getApartment());
                schedule.setTechnician(this.technicianService.getTechnicianById(index));
                this.scheduleRepository.saveAndFlush(schedule);

                this.requestService.updateRequestStatusReadyToService(plan.getRequest());
            }
            index++;
        }
    }

    private void saveLowestTechnicianPlan(List<TechnicianPlanDto> lowestTechnicianPlan) {
        for (TechnicianPlanDto planDto: lowestTechnicianPlan) {
            Schedule schedule = new Schedule();
            schedule.setRequest(planDto.getRequest());
            schedule.setApartment(planDto.getApartment());
            schedule.setTechnician(this.technicianService.getLowestTechnician());
            this.scheduleRepository.saveAndFlush(schedule);

            this.requestService.updateRequestStatusReadyToService(planDto.getRequest());
        }

        int priorityHour = lowestTechnicianPlan.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority())).map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();
        this.configService.updatePriorityHour(priorityHour);
    }

    private List<TechnicianPlanDto> checkBestRequest(List<List<TechnicianPlanDto>> otherPlanList) {
        List<TechnicianPlanDto> bestRequest = new ArrayList<>();
        int maxNum = -1;
        for (List<TechnicianPlanDto> plan: otherPlanList) {
            int numberOfPriority4 = plan.stream().map(TechnicianPlanDto::getPriority).filter(priority -> priority == 4).toList().size();
            if (numberOfPriority4 > maxNum) {
                maxNum = numberOfPriority4;
                bestRequest.clear();
                bestRequest.addAll(plan);
                bestRequestPlanId = otherPlanList.indexOf(plan);
            }
        }

        return bestRequest;
    }

    private void findPossibleRequest(List<List<TechnicianPlanDto>> res, List<TechnicianPlanDto> ds, int target, List<TechnicianPlanDto> arr, int index, Integer[] rangePriority){
        if (target == 0) {
            int numOfApartment = this.findNumberOfApartment(ds);
            int numOfPriority = this.findNumberOfPriority(ds, MOST_PRIORITY);

            if (numOfPriority >= rangePriority[0] && numOfPriority <= rangePriority[1]) {
                if (numOfApartment < totalApartment) {
                    totalApartment = numOfApartment;
                    res.clear();
                    res.add(new ArrayList<>(ds));
                } else if (numOfApartment == totalApartment) {
                    res.add(new ArrayList<>(ds));
                }
            }
            return;
        }

        for (int i= index ; i<arr.size(); i++) {
            if (arr.get(i).getEstimateTime() > target)
                break;

            ds.add(arr.get(i));
            findPossibleRequest(res, ds, target-arr.get(i).getEstimateTime() , arr, i+1, rangePriority);
            ds.remove(ds.size()-1 );
        }
    }

    private void findPossibleOtherRequire2Request(List<List<TechnicianPlanDto>> res, List<TechnicianPlanDto> ds, int target, List<TechnicianPlanDto> arr, int index){
        if (target == 0) {
            int numOfApartment = this.findNumberOfApartment(ds);

            if (numOfApartment < totalApartment) {
                totalApartment = numOfApartment;
                res.clear();
                res.add(new ArrayList<>(ds));
            } else if (numOfApartment == totalApartment) {
                res.add(new ArrayList<>(ds));
            }
            return;
        }

        for (int i= index ; i<arr.size(); i++) {
            if (arr.get(i).getEstimateTime() > target)
                break;

            ds.add(arr.get(i));
            findPossibleOtherRequire2Request(res, ds, target-arr.get(i).getEstimateTime() , arr, i+1);
            ds.remove(ds.size()-1 );
        }
    }

    private void findPossibleRemainingRequest(List<List<TechnicianPlanDto>> res, List<TechnicianPlanDto> ds, int target, List<TechnicianPlanDto> arr, int index, List<Integer> apartment) {
        if (!keepworking) {
            return;
        }

        if (target == 0) {
            int remainingNumberOfApartment = ds.stream().map(TechnicianPlanDto::getApartmentId).filter(Predicate.not(apartment::contains)).toList().size();
            long sumDate = ds.stream().map(d -> d.getRequest().getRequestDate()).mapToLong(Date::getTime).sum();

            if (remainingNumberOfApartment == 0) {
                res.clear();
                res.add(new ArrayList<>(ds));
                keepworking = false;
            } else if (remainingNumberOfApartment < totalApartment) {
                totalApartment = remainingNumberOfApartment;
                totalDate = sumDate;
                res.clear();
                res.add(new ArrayList<>(ds));
            } else if (remainingNumberOfApartment == totalApartment && sumDate < totalDate) {
                totalDate = sumDate;
                res.clear();
                res.add(new ArrayList<>(ds));
            }
            return;
        }

        for (int i=index; i<arr.size(); i++) {
            if (arr.get(i).getEstimateTime() > target)
                break;

            
            ds.add(arr.get(i));
            findPossibleRemainingRequest(res, ds, target-arr.get(i).getEstimateTime() , arr, i+1, apartment);
            ds.remove(ds.size() - 1);
        }
    }

    private void findPossibleRemainingPriorityRequest(List<List<TechnicianPlanDto>> res, List<TechnicianPlanDto> ds, int target, List<TechnicianPlanDto> arr, int index, List<Integer> apartment) {
        if (!keepworking) {
            return;
        }

        if (target == 0) {
            int remainingNumberOfApartment = ds.stream().map(TechnicianPlanDto::getApartmentId).filter(Predicate.not(apartment::contains)).toList().size();
            int sumPriority = ds.stream().map(TechnicianPlanDto::getPriority).mapToInt(Integer::intValue).sum();

            if (remainingNumberOfApartment == 0) {
                res.clear();
                res.add(new ArrayList<>(ds));
                keepworking = false;
            } else if (remainingNumberOfApartment < totalApartment) {
                totalApartment = remainingNumberOfApartment;
                totalPriority = sumPriority;
                res.clear();
                res.add(new ArrayList<>(ds));
            } else if (remainingNumberOfApartment == totalApartment && sumPriority < totalPriority) {
                totalPriority = sumPriority;
                res.clear();
                res.add(new ArrayList<>(ds));
            }
            return;
        }

        for (int i=index; i<arr.size(); i++) {
            if (arr.get(i).getEstimateTime() > target)
                break;


            ds.add(arr.get(i));
            findPossibleRemainingPriorityRequest(res, ds, target-arr.get(i).getEstimateTime() , arr, i+1, apartment);
            ds.remove(ds.size() - 1);
        }
    }

    private void findPossibleOnePriorityRequest(List<List<TechnicianPlanDto>> res, List<TechnicianPlanDto> ds, int target, List<TechnicianPlanDto> arr, int index) {
        if (target == 0) {
            int numOfApartment = this.findNumberOfApartment(ds);
            int sumPriority = ds.stream().map(TechnicianPlanDto::getPriority).mapToInt(Integer::intValue).sum();

            if (sumPriority < totalPriority) {
                totalPriority = sumPriority;
                res.clear();
                res.add(new ArrayList<>(ds));
            } else if (sumPriority == totalPriority && numOfApartment < totalApartment) {
                totalApartment = numOfApartment;
                res.clear();
                res.add(new ArrayList<>(ds));
            }
            return;
        }

        for (int i=index; i<arr.size(); i++) {
            if (arr.get(i).getEstimateTime() > target)
                break;

            ds.add(arr.get(i));
            findPossibleOnePriorityRequest(res, ds, target-arr.get(i).getEstimateTime() , arr, i+1);
            ds.remove(ds.size()-1 );
        }
    }

    private List<TechnicianPlanDto> findRequestList(int totalPriorityHour, int totalTargetHour, int totalRequestHour, List<TechnicianPlanDto> requestListForPlan, List<TechnicianPlanDto> priorityRequestList) {
        if (totalPriorityHour == totalTargetHour) {
            return priorityRequestList;
        }

        if (totalRequestHour <= totalTargetHour) {
            return requestListForPlan;
        }

        List<List<TechnicianPlanDto>> possibleRequestList = new ArrayList<>();
        List<TechnicianPlanDto> possibleRequest = new ArrayList<>();
        if (totalPriorityHour > totalTargetHour) {
            List<TechnicianPlanDto> sortedPriorityRequestList = priorityRequestList.stream().sorted(Comparator.comparing(TechnicianPlanDto::getEstimateTime)).toList();
            totalApartment = 6;
            totalPriority = Integer.MAX_VALUE;
            findPossibleOnePriorityRequest(possibleRequestList, possibleRequest, totalTargetHour, sortedPriorityRequestList, 0);

            return possibleRequestList.get(0);
        }

        List<TechnicianPlanDto> normalRequest = requestListForPlan.stream().filter(Predicate.not(priorityRequestList::contains)).toList();
        List<Integer> apartmentIds = priorityRequestList.stream().map(TechnicianPlanDto::getApartmentId).distinct().toList();

        totalApartment = normalRequest.size();
        totalDate = Long.MAX_VALUE;
        keepworking = true;

        int priorityHour = priorityRequestList.stream().map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();
        int targetNormalHour = totalTargetHour - priorityHour;

        List<TechnicianPlanDto> sortedNormalRequest = normalRequest.stream().sorted(Comparator.comparing(TechnicianPlanDto::getEstimateTime)).toList();
        findPossibleRemainingRequest(possibleRequestList, possibleRequest, targetNormalHour, sortedNormalRequest, 0, apartmentIds);

        List<TechnicianPlanDto> requestList = new ArrayList<>(priorityRequestList);
        requestList.addAll(possibleRequestList.get(0));

        return requestList;
    }

    private List<TechnicianPlanDto> findRequire2RequestList(Integer[] targetHour, List<TechnicianPlanDto> requestListForPlan) {
        List<TechnicianPlanDto> priorityRequest = requestListForPlan.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority())).toList();
        List<TechnicianPlanDto> priorityRequire2Request = priorityRequest.stream().filter(req -> req.getRequest().getEstimateTechnician() > 1).toList();
        List<TechnicianPlanDto> normalRequest = requestListForPlan.stream().filter(Predicate.not(priorityRequest::contains)).sorted(Comparator.comparing(TechnicianPlanDto::getEstimateTime)).toList();
        List<TechnicianPlanDto> normalRequire2Request = normalRequest.stream().filter(req -> req.getRequest().getEstimateTechnician() > 1).toList();

        int require2PriorityHour = priorityRequire2Request.stream().map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();
        int priorityHour = priorityRequest.stream().map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();

        List<List<TechnicianPlanDto>> tempList = new ArrayList<>();
        List<TechnicianPlanDto> temp = new ArrayList<>();

        keepworking = true;
        totalApartment = Integer.MAX_VALUE;
        totalDate = Long.MAX_VALUE;
        totalPriority = Integer.MAX_VALUE;

        if (require2PriorityHour > targetHour[0]) {
            findPossibleOnePriorityRequest(tempList, temp, targetHour[0], priorityRequire2Request, 0);
            return tempList.get(0);
        } else if (require2PriorityHour < targetHour[0]) {
            int remainingHourForTechnician1 = targetHour[0] - require2PriorityHour;
            int remainingHourForTechnician2 = targetHour[1] - require2PriorityHour;
            List<TechnicianPlanDto> returnList = new ArrayList<>(priorityRequire2Request);

            List<Integer> apartmentIds = findApartmentId(new ArrayList<>(), returnList);

            if (priorityHour == 0) {
                findPossibleRemainingRequest(tempList, temp, remainingHourForTechnician1, normalRequire2Request, 0, apartmentIds);
                returnList.addAll(tempList.get(0));
                return returnList;
            }

            if (priorityHour > remainingHourForTechnician1) {
                findPossibleRemainingPriorityRequest(tempList, temp, remainingHourForTechnician1, priorityRequest, 0, apartmentIds);
                returnList.addAll(tempList.get(0));

                keepworking = true;
                totalApartment = Integer.MAX_VALUE;
                totalDate = Long.MAX_VALUE;
                totalPriority = Integer.MAX_VALUE;

                List<TechnicianPlanDto> remainingPriorityRequest = priorityRequest.stream().filter(Predicate.not(tempList.get(0)::contains)).toList();
                int remainingPriorityHour = remainingHourForTechnician1 - priorityHour;
                apartmentIds = findApartmentId(returnList, new ArrayList<>());
                if (remainingPriorityHour > remainingHourForTechnician2) {
                    tempList.clear();
                    temp.clear();
                    findPossibleRemainingPriorityRequest(tempList, temp, remainingHourForTechnician2, remainingPriorityRequest, 0, apartmentIds);
                    returnList.addAll(tempList.get(0));
                } else {
                    returnList.addAll(remainingPriorityRequest);

                    apartmentIds = findApartmentId(returnList, new ArrayList<>());
                    if (remainingPriorityHour < remainingHourForTechnician2) {
                        tempList.clear();
                        temp.clear();
                        findPossibleRemainingRequest(tempList, temp, remainingHourForTechnician2 - remainingPriorityHour, normalRequest, 0, apartmentIds);
                        returnList.addAll(tempList.get(0));
                    }
                }
            } else {
                returnList.addAll(priorityRequest);

                if (priorityHour < remainingHourForTechnician1) {
                    apartmentIds = findApartmentId(returnList, new ArrayList<>());
                    findPossibleRemainingRequest(tempList, temp, remainingHourForTechnician2 + (remainingHourForTechnician1 - priorityHour), normalRequest, 0, apartmentIds);
                    returnList.addAll(tempList.get(0));
                }
            }

            return returnList;
        } else {
            return priorityRequire2Request;
        }
    }

    private List<TechnicianPlanDto> findOtherRequestList(List<TechnicianPlanDto> allRequest, List<TechnicianPlanDto> lowestPlan, Integer[] targetHour) {
        List<TechnicianPlanDto> remainingRequest = allRequest.stream().filter(Predicate.not(lowestPlan::contains)).toList();
        List<TechnicianPlanDto> priorityRequest = remainingRequest.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority())).sorted(Comparator.comparing(TechnicianPlanDto::getEstimateTime)).toList();

        // TODO: what if priority hour is more than target hour

        List<Integer> apartmentIds;
        keepworking = true;

        int priorityHour = priorityRequest.stream().map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();
        int totalTargetHour = targetHour[0] + targetHour[1];

        if (priorityHour == totalTargetHour) {
            return priorityRequest;
        }

        List<List<TechnicianPlanDto>> tempPossibleOtherPlanList = new ArrayList<>();
        List<TechnicianPlanDto> tempPossibleOtherPlan = new ArrayList<>();
        if (priorityHour > totalTargetHour) {
            apartmentIds = findApartmentId(lowestPlan, new ArrayList<>());
            totalApartment = priorityRequest.size();
            totalPriority = Integer.MAX_VALUE;
            findPossibleRemainingPriorityRequest(tempPossibleOtherPlanList, tempPossibleOtherPlan, totalTargetHour, priorityRequest, 0, apartmentIds);

            return tempPossibleOtherPlanList.get(0);
        } else {
            List<TechnicianPlanDto> normalRequest = remainingRequest.stream().filter(Predicate.not(priorityRequest::contains)).sorted(Comparator.comparing(TechnicianPlanDto::getEstimateTime)).toList();
            int normalTargetHour = totalTargetHour - priorityHour;

            apartmentIds = findApartmentId(lowestPlan, priorityRequest);
            totalApartment = normalRequest.size();
            totalDate = Long.MAX_VALUE;
            findPossibleRemainingRequest(tempPossibleOtherPlanList, tempPossibleOtherPlan, normalTargetHour, normalRequest, 0, apartmentIds);

            List<TechnicianPlanDto> possibleRemainingRequest = new ArrayList<>(priorityRequest);
            possibleRemainingRequest.addAll(tempPossibleOtherPlanList.get(0));

            return possibleRemainingRequest;
        }
    }

    private List<TechnicianPlanDto> findOtherRequire2RequestList(List<TechnicianPlanDto> requestListForPlan, List<TechnicianPlanDto> possibleLowestPlan, Integer[] targetHour) {
        List<TechnicianPlanDto> remainingRequest = requestListForPlan.stream().filter(Predicate.not(possibleLowestPlan::contains)).toList();
        List<TechnicianPlanDto> remainingRequire1Request = remainingRequest.stream().filter(req -> req.getRequest().getEstimateTechnician() == 1).toList();
        List<TechnicianPlanDto> remainingRequire2Request = remainingRequest.stream().filter(req -> req.getRequest().getEstimateTechnician() > 1).toList();
        List<TechnicianPlanDto> priorityRequire2Request = remainingRequire2Request.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority())).toList();
        List<TechnicianPlanDto> normalRequire2Request = remainingRequire2Request.stream().filter(Predicate.not(priorityRequire2Request::contains)).toList();

        int require2PriorityHour = priorityRequire2Request.stream().map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();

        List<List<TechnicianPlanDto>> tempList = new ArrayList<>();
        List<TechnicianPlanDto> temp = new ArrayList<>();
        List<Integer> apartmentIds;

        keepworking = true;
        totalApartment = Integer.MAX_VALUE;
        totalDate = Long.MAX_VALUE;
        totalPriority = Integer.MAX_VALUE;

        if (require2PriorityHour > targetHour[0]) {
            apartmentIds = findApartmentId(possibleLowestPlan, new ArrayList<>());
            findPossibleRemainingPriorityRequest(tempList, temp, targetHour[0], priorityRequire2Request, 0, apartmentIds);

            return tempList.get(0);
        } else if (require2PriorityHour < targetHour[0]) {
            int remainingHourForTechnician1 = targetHour[0] - require2PriorityHour;
            int remainingHourForTechnician2 = targetHour[1] - require2PriorityHour;
            List<TechnicianPlanDto> returnList = new ArrayList<>(priorityRequire2Request);

            apartmentIds = findApartmentId(possibleLowestPlan, returnList);

            // TODO: เช็คตรงนี้ดีๆ ต้อง filter แต่ที่เวลาน้อยกว่า remaining
            List<TechnicianPlanDto> priorityRequest = remainingRequire1Request.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority()) && req.getEstimateTime() <= remainingHourForTechnician1).toList();
            List<TechnicianPlanDto> normalRequest = remainingRequire1Request.stream().filter(Predicate.not(priorityRequest::contains)).sorted(Comparator.comparing(TechnicianPlanDto::getEstimateTime)).toList();
            int priorityHour = priorityRequest.stream().map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();

            if (priorityHour == 0) {
                findPossibleRemainingRequest(tempList, temp, remainingHourForTechnician1, normalRequire2Request, 0, apartmentIds);
                returnList.addAll(tempList.get(0));
                return returnList;
            }

            if (priorityHour > remainingHourForTechnician1) {
                findPossibleRemainingPriorityRequest(tempList, temp, remainingHourForTechnician1, priorityRequest, 0, apartmentIds);
                returnList.addAll(tempList.get(0));

                List<TechnicianPlanDto> remainingPriorityRequest = priorityRequest.stream().filter(Predicate.not(tempList.get(0)::contains)).toList();
                int remainingPriorityHour = priorityHour - remainingHourForTechnician1;
                keepworking = true;
                totalApartment = Integer.MAX_VALUE;
                totalDate = Long.MAX_VALUE;
                totalPriority = Integer.MAX_VALUE;
                if (remainingPriorityHour > remainingHourForTechnician2) {
                    tempList.clear();
                    temp.clear();
                    findPossibleRemainingPriorityRequest(tempList, temp, remainingHourForTechnician2, remainingPriorityRequest, 0, apartmentIds);
                    returnList.addAll(tempList.get(0));
                } else {
                    returnList.addAll(remainingPriorityRequest);
                    if (remainingPriorityHour < remainingHourForTechnician2) {
                        tempList.clear();
                        temp.clear();
                        findPossibleRemainingRequest(tempList, temp, remainingHourForTechnician2 - remainingPriorityHour, normalRequest, 0, apartmentIds);
                        returnList.addAll(tempList.get(0));
                    }
                }
            } else {
                returnList.addAll(priorityRequest);
                if (priorityHour < remainingHourForTechnician1) {
                    findPossibleRemainingRequest(tempList, temp, (remainingHourForTechnician1 - priorityHour) + remainingHourForTechnician2, normalRequest, 0, apartmentIds);
                    returnList.addAll(tempList.get(0));
                }
            }

            return returnList;
        } else {
            return priorityRequire2Request;
        }
    }

    private List<Integer> findApartmentId(List<TechnicianPlanDto> planList, List<TechnicianPlanDto> priorityRequest) {
        List<TechnicianPlanDto> allRequest = new ArrayList<>();
        if (!planList.isEmpty() && !priorityRequest.isEmpty()) {
            allRequest.addAll(planList);
            allRequest.addAll(priorityRequest);
        } else if (priorityRequest.isEmpty()) {
            allRequest.addAll(planList);
        } else {
            allRequest.addAll(priorityRequest);
        }

        return allRequest.stream().map(TechnicianPlanDto::getApartmentId).distinct().toList();
    }

    private int findNumberOfApartment(List<TechnicianPlanDto> technicianPlanDtoList) {
        int cnt = 0;
        int temp = 0;

        List<TechnicianPlanDto> planDtos = technicianPlanDtoList.stream().sorted(Comparator.comparingInt(TechnicianPlanDto::getApartmentId)).toList();

        for (TechnicianPlanDto planDto : planDtos) {
            if (planDto.getApartmentId() != temp) {
                temp = planDto.getApartmentId();
                cnt++;
            }
        }

        return cnt;
    }

    private int findNumberOfPriority(List<TechnicianPlanDto> technicianPlanDtoList, List<Integer> priority) {
        int cnt = 0;

        for (TechnicianPlanDto planDto : technicianPlanDtoList) {
            if (priority.contains(planDto.getPriority())) {
                cnt = cnt + planDto.getEstimateTime();
            }
        }

        return cnt;
    }
}

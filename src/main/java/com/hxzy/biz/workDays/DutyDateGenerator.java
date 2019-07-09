package com.hxzy.biz.workDays;

import com.hxzy.bean.Holiday;
import com.hxzy.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DutyDateGenerator {
    /**
     * 日期格式：yyyy-MM
     * @param dateStr  生成值班的日期
     * @return 值班日期
     */
    public static List<Date> getDudyDate(String dateStr) {

        StringUtil.validateDateStr(dateStr);

        List<Date> list = new ArrayList<>();

        Calendar c = Calendar.getInstance(); //获取当前日期

        Date date = StringUtil.formatStr2DateMonth(dateStr); //将日期字符串转换为日期
        c.setTime(date);

        int actualMaximum = c.getActualMaximum(Calendar.DATE); //每个月的最后一天

        int theFirstDateOfMonth = findTheFirstDateOfMonth(c); //每个月的第一个工作日

        c.set(Calendar.DATE, theFirstDateOfMonth);// 每个月的1号为值班起点

        list.add(c.getTime());

        List<Date> list2 = Stream.generate(() -> {

            c.add(Calendar.DATE, 1);

            return c.getTime();
        })
                .limit(actualMaximum - theFirstDateOfMonth)
                .collect(Collectors.toList());

        list.addAll(list2); //合并集合

        return list.stream()
                //.filter(t -> isWorkOnHoliday(t)) //排除周末补班的情况
                //.filter(t -> isNotWeekDay(t))  //排除周末
                //.filter(t -> isNotHoliday(t))  //排除法定节日
                .filter(t ->
                        isWorkOnHoliday(t) || isNotWeekDay(t) && isNotHoliday(t)
                )
                .collect(Collectors.toList());
    }

    private static boolean isNotWeekDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY;
    }

    private static boolean isNotHoliday(Date date) {
        List<Holiday> holiday = LegalHoliday.getInstance().getHoliday();
        return !holiday.stream()
                .map(t -> {
                    try {
                        //此处使用原型模式得到holiday对象
                        //法定假日后的上班时间为配置文件中to对应日期+1天
                        Holiday clone = t.clone();
                        Calendar instance = Calendar.getInstance();
                        instance.setTimeInMillis(clone.getTo());
                        instance.add(Calendar.DATE, 1);
                        clone.setTo(instance.getTimeInMillis());
                        return clone;
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .anyMatch(t ->
                        //在这个表达式范围内的日期为放假日期
                        date.getTime() >= t.getFrom() && date.getTime() < t.getTo()
                );
    }

    public static boolean isWorkOnHoliday(Date date) {
        List<Holiday> workDay = WorkOnHolidays.getInstance().getHoliday();

        return workDay.stream()
                .filter(t ->
                        {
                            Calendar c1 = Calendar.getInstance();
                            Calendar c2 = Calendar.getInstance();
                            Calendar c3 = Calendar.getInstance();

                            c1.setTime(date);
                            c2.setTimeInMillis(t.getFrom());
                            c3.setTimeInMillis(t.getTo());
                            
                            int date_month = c1.get(Calendar.MONTH);
                            int c2_month = c2.get(Calendar.MONTH);
                            int c3_month = c3.get(Calendar.MONTH);
                            
                            return date_month == c2_month && date_month == c3_month || date_month == c2_month && date_month == c3_month - 1; 
                            
                            //return c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) || c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) - 1;
                        }
                )
                .anyMatch(t ->
                        date.getTime() >= t.getFrom() && date.getTime() <= t.getTo()
                );
    }

    public static int findTheFirstDateOfMonth(Calendar c){
        int month = c.get(Calendar.MONTH);//生成值班表的月份
        List<Holiday> holiday = LegalHoliday.getInstance().getHoliday();
        Optional<Holiday> first = holiday.stream().filter(t -> {
            long to = t.getTo();
            Calendar instance = Calendar.getInstance();
            instance.setTimeInMillis(to);
            int to_month = instance.get(Calendar.MONTH);

            long from = t.getFrom();
            instance.setTimeInMillis(from);
            int from_month = instance.get(Calendar.MONTH);
            /*
             * 放假持续到第二个月
             */
            return from_month == month - 1 && to_month == month;
        }).min((o1, o2) -> (int) (o1.getTo() - o2.getTo()));

        if (first.isPresent()) {
            long to = first.get().getTo();
            Calendar instance = Calendar.getInstance();
            instance.setTimeInMillis(to);
            return instance.get(Calendar.DATE) + 1;
        }
        return 1;
    }


}

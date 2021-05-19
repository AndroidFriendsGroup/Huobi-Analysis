package com.razerdp.huobi.analysis.utils;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.StringDef;

import com.razerdp.huobi_analysis.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by 大灯泡 on 2019/4/22.
 */

public class TimeUtil {

    public final static String YYYYMMDD_SIM = "yyyyMMdd";

    public static int getWeekDateIndex(long time) {
        Calendar cal = Calendar.getInstance(); // 获得一个日历
        Date date = new Date(time);
        cal.setTime(date);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1; // 指示一个星期中的某天。
        if (w < 0)
            w = 0;
        return w;
    }

    public final static String YYYYMMDDTHHMMSS = "yyyy-MM-ddTHH:mm:ss";
    public final static String YYYYMMDD = "yyyy-MM-dd";
    public final static String YYYYMD = "yyyyMMdd";
    public final static String YYYYMM = "yyyy-MM";
    public final static String HHMM = "HH:mm";
    public final static String YYYYMMDDHHMM = "yyyy-MM-dd HH:mm";
    public final static String MMDDHHMM = "MM-dd HH:mm";
    public final static String YYYYMMDDHHMMSS = "yyyy-MM-dd HH:mm:ss";
    public final static String MMDDHHMM_CHINESE = "MM月dd日 HH:mm";
    public final static String MMDDHHMM_CHINESE_SHORT = "MM月dd日";
    public final static String YYYYMMDD_CHINESE = "yyyy年MM月dd日";
    public final static String YYYYMMDD_SLASH = "yyyy/MM/dd";
    public final static String YYYYMM_SLASH = "yyyy/MM";
    public final static String MMDD = "MM-dd";
    public final static String YYYYMMDD_DOT = "yyyy.MM.dd";
    public final static String YYYYMMDD_HHMMSS = "yyyyMMdd_HHmmss";
    public final static String YYYYMMDD_HHMM = "yyyyMMdd HH:mm";
    public final static String HHMMSS_YYMMDD = "HH:mm:ss yyyy-MM-dd";


    public static final long YEAR = 365 * 24 * 60 * 60;// 年
    public static final long MONTH = 30 * 24 * 60 * 60;// 月
    public static final long WEEK = 7 * 24 * 60 * 60;// 天
    public static final long DAY = 24 * 60 * 60;// 天
    public static final long HOUR = 60 * 60;// 小时
    public static final long MINUTE = 60;// 分钟
    public static final long SECOND = 1;// 秒

    public static final String[] WEEKS = StringUtil.getStringArray(R.array.arrays_weeks);


    public static String commonTranslate(long time) {
        time = toMilliseconds(time);
        long curTime = System.currentTimeMillis();
        int sub = getSubDay(time, curTime);
        long subTime = curTime - time;
        if (sub < 1) {
            //当天消息
            if (subTime <= MINUTE * 1000) {
                //1分钟内->刚刚
                return StringUtil.getString(R.string.lib_time_level1);
            } else if (subTime < HOUR * 1000) {
                //1分钟~1小时->xx分钟前
                return StringUtil.getString(R.string.lib_time_level2, subTime / (MINUTE * 1000));
            } else {
                //1小时~1天
                return longToTimeStr(time, HHMM);
            }
        } else if (sub <= 7) {
            //1周内
            if (sub == 1) {
                //昨天
                return StringUtil.getString(R.string.lib_time_level3);
            } else {
                //星期
                return getWeekDate(time);
            }
        } else {
            if (NumberUtils.inRange(sub, 7, 365)) {
                //大于一周，小于一年
                return TimeUtil.longToTimeStr(time, TimeUtil.MMDDHHMM_CHINESE);
            }
            return TimeUtil.longToTimeStr(time, TimeUtil.YYYYMMDDHHMM);
        }
    }

    /**
     * 获取未来 第 past 天的日期
     *
     * @param past
     * @return
     */
    public static String getFetureDate(int past) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + past);
        Date today = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String result = format.format(today);
        Log.e(null, result);
        return result;
    }

    /**
     * 获取未来 第 past 天的日期
     *
     * @param past
     * @return
     */
    public static String getFetureDate(int past, String dateFormat) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + past);
        Date today = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        String result = format.format(today);
        return result;
    }

    public static String getWhichDay(int past) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + past);
        Date today = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat("EEEE");
        String result = format.format(today);
        return result;
    }

    public static String formatDate(String date, String dateFormat) throws ParseException {
        SimpleDateFormat inputSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date dateTime = inputSdf.parse(date);
        SimpleDateFormat ouputSdf = new SimpleDateFormat(dateFormat);
        return ouputSdf.format(dateTime);
    }

    /**
     * 获取未来 第 past 天的日期
     *
     * @param date 20170412
     * @return 04-12
     */
    public static String formatDate(String date) {
        if (!TextUtils.isEmpty(date) && date.length() == 8) {
            return new StringBuilder(date.substring(4)).insert(2, "-").toString();
        }
        return date;
    }

    /**
     * 时间戳转格式化日期
     *
     * @param timestamp 单位毫秒
     * @param format    日期格式
     * @return
     */
    public static String longToTimeStr(long timestamp, @FormateType String format) {
        return transferLongToDate(timestamp, format);
    }

    /**
     * 时间戳转格式化日期
     *
     * @param timestamp 单位毫秒
     * @param format    日期格式
     * @return
     */
    private static String transferLongToDate(long timestamp, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            Date date = new Date(toMilliseconds(timestamp));
            return sdf.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return "null";
        }
    }

    /**
     * 格式化日期转时间戳
     *
     * @param format 日期格式
     * @return
     */
    public static long strToTimestamp(String date, String format) {
        long timestamp = 0;
        try {
            timestamp = new SimpleDateFormat(format).parse(date).getTime() / 1000;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return timestamp;
    }

    public static String transferDateFormat(String originalDate, @FormateType String oldFormate, @FormateType String newFormate) {
        long time = stringToTimeStamp(originalDate, oldFormate);
        return transferDateFormat(time, newFormate);
    }

    public static String transferDateFormat(long originalDate, @FormateType String newFormate) {
        return longToTimeStr(originalDate, newFormate);
    }

    public static String transferDateFormat(String originalDate, @FormateType String newFormate) {
        return transferDateFormat(originalDate, getTimeFormatPatten(originalDate), newFormate);
    }

    public static long stringToTimeStamp(String timeString) {
        String patten = getTimeFormatPatten(timeString);
        if (TextUtils.isEmpty(patten)) return 0;
        SimpleDateFormat sdf = new SimpleDateFormat(patten);
        try {
            return sdf.parse(timeString).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static long stringToTimeStamp(String timeString, @FormateType String format) {
        if (TextUtils.isEmpty(timeString) || TextUtils.isEmpty(format)) return 0;
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            return sdf.parse(timeString).getTime();
        } catch (ParseException | NullPointerException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static long stringToTimeStamp(String timeString, @FormateType String format, TimeZone timeZone) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
        sdf.setTimeZone(timeZone);
        try {
            return sdf.parse(timeString).getTime();
        } catch (ParseException | NullPointerException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static boolean isToday(long timeInMilis) {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(timeInMilis);
        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.DATE) == time.get(Calendar.DATE)) {
            return true;
        } else {
            return false;
        }
    }

    public static int getSubDay(long start, long end) {
        int result = Math.round((end - start) / (1000 * DAY));
        return result < 0 ? -1 : result;
    }

    public static String getTimeFormatPatten(long time) {
        return TimeFormatChecker.getTimeFormatPatten(time);
    }

    public static String getTimeFormatPatten(String time) {
        return TimeFormatChecker.getTimeFormatPatten(time);
    }

    public static String getWeekDate(long time) {
        Calendar cal = Calendar.getInstance(); // 获得一个日历
        Date date = new Date(time);
        cal.setTime(date);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1; // 指示一个星期中的某天。
        if (w < 0)
            w = 0;
        return WEEKS[w];
    }

    public static long toMilliseconds(long from) {
        if (from >= 1000000000000L) {
            return from;
        }
        return TimeUnit.SECONDS.toMillis(from);
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({YYYYMMDD, YYYYMM, MMDDHHMM,
            HHMM, YYYYMMDDHHMM, YYYYMMDDHHMMSS,
            MMDDHHMM_CHINESE, MMDDHHMM_CHINESE_SHORT, YYYYMMDD_CHINESE,
            YYYYMMDD_SLASH, YYYYMM_SLASH, MMDD,
            YYYYMMDD_DOT, YYYYMD, YYYYMMDD_HHMMSS, YYYYMMDD_SIM,
            HHMMSS_YYMMDD, YYYYMMDD_HHMM, YYYYMMDDTHHMMSS})
    public @interface FormateType {
    }

    static class TimeFormatChecker {
        static SimpleDateFormat mCheckFormater = new SimpleDateFormat();
        static final String[] patten = {YYYYMMDD, YYYYMM, MMDDHHMM,
                HHMM, YYYYMMDDHHMM, YYYYMMDDHHMMSS,
                MMDDHHMM_CHINESE, MMDDHHMM_CHINESE_SHORT, YYYYMMDD_CHINESE,
                YYYYMMDD_SLASH, YYYYMM_SLASH, MMDD,
                YYYYMMDD_DOT, YYYYMD, YYYYMMDD_HHMMSS, YYYYMMDD_SIM, HHMMSS_YYMMDD,
                YYYYMMDD_HHMM,YYYYMMDDTHHMMSS};

        public static String getTimeFormatPatten(long time) {
            String result = "";
            for (String s : patten) {
                mCheckFormater.applyPattern(s);
                try {
                    result = mCheckFormater.format(time);
                    if (!TextUtils.isEmpty(result)) {
                        return s;
                    }
                } catch (Exception e) {
                }
            }
            return null;
        }

        public static String getTimeFormatPatten(String time) {
            long result = 0;
            for (String s : patten) {
                mCheckFormater.applyPattern(s);
                try {
                    result = mCheckFormater.parse(time).getTime();
                    if (result != 0) {
                        return s;
                    }
                } catch (Exception e) {
                }
            }
            return null;
        }


    }
}

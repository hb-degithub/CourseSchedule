package com.hbde.courseschedule.service.liveactivity

/**
 * 实时活动状态密封类
 * 用于描述当前课程的实时状态
 */
sealed class LiveActivityStatus {

    /**
     * 倒计时状态
     * @param minutes 距离课程开始的剩余分钟数
     */
    data class Countdown(val minutes: Int) : LiveActivityStatus()

    /**
     * 正在上课状态
     * @param courseName 课程名称
     * @param classroom 教室位置
     */
    data class InClass(val courseName: String, val classroom: String?) : LiveActivityStatus()

    /**
     * 课间休息状态
     * @param nextCourseName 下一节课名称
     * @param minutes 距离下一节课开始的剩余分钟数
     */
    data class Break(val nextCourseName: String, val minutes: Int) : LiveActivityStatus()
}

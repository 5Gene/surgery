package osp.surgery.plugin.plan

import osp.surgery.api.ClassDoctor
import osp.surgery.api.FilterAction

/**
 * 过滤结果数据类
 * 避免多次groupBy操作，提高性能
 * 
 * @author yun.
 * @date 2024
 */
data class FilterResult<DOCTOR : ClassDoctor>(
    val now: List<DOCTOR> = emptyList(),
    val last: List<DOCTOR> = emptyList(),
    val none: List<DOCTOR> = emptyList()
) {
    /**
     * 是否有需要处理的Doctor
     */
    val hasTransform: Boolean
        get() = now.isNotEmpty() || last.isNotEmpty()
    
    /**
     * 获取需要处理的Doctor列表（now + last）
     */
    val allTransform: List<DOCTOR>
        get() = now + last
}

/**
 * 一次性过滤Doctors，避免多次groupBy
 */
fun <DOCTOR : ClassDoctor> filterDoctors(
    doctors: List<DOCTOR>,
    filter: (DOCTOR) -> FilterAction
): FilterResult<DOCTOR> {
    val result = FilterResultBuilder<DOCTOR>()
    doctors.forEach { doctor ->
        when (filter(doctor)) {
            FilterAction.transformNow -> result.now.add(doctor)
            FilterAction.transformLast -> result.last.add(doctor)
            FilterAction.noTransform -> result.none.add(doctor)
        }
    }
    return result.build()
}

/**
 * FilterResult构建器
 */
private class FilterResultBuilder<DOCTOR : ClassDoctor> {
    val now = mutableListOf<DOCTOR>()
    val last = mutableListOf<DOCTOR>()
    val none = mutableListOf<DOCTOR>()
    
    fun build(): FilterResult<DOCTOR> {
        return FilterResult(now, last, none)
    }
}


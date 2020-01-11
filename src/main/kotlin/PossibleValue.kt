package dev.mee42

class PossibleValue<V,E> private constructor(private val success: V?, private val fail: E?){
    companion object {
        fun <A,E> success(a: A): PossibleValue<A,E> = PossibleValue(success = a,   fail = null)
        fun <A,E> fail(e: E):    PossibleValue<A,E> = PossibleValue(success = null,fail =  e)
    }
    fun isFail() = fail != null
    fun isSuccess() = success != null
    inline fun <R> ifFail( block: (E) -> R):R? {
        if(isFail()) return block(getFailUnsafe())
        return null
    }
    inline fun ifFailReturn(block: (E) -> Nothing): V {
        if(isFail()){
            block(getFailUnsafe())
        } else {
            return getSuccessUnsafe()
        }
    }
    inline fun ifSuccessReturn(block: (V) -> Nothing): E {
        if(isSuccess()){
            block(getSuccessUnsafe())
        } else {
            return getFailUnsafe()
        }
    }


    inline fun <R> ifSuccess(block: (V) -> R):R? {
        if(isSuccess()) return block(getSuccessUnsafe())
        return null
    }


    fun getFailUnsafe() = fail!!
    fun getSuccessUnsafe() = success!!
}

fun <V,E> E.asFail(): PossibleValue<V, E> =  PossibleValue.fail(this)
fun <V,E> V.asSuccess(): PossibleValue<V, E> = PossibleValue.success(this)
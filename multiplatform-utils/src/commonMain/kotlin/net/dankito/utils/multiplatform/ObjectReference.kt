package net.dankito.utils.multiplatform


open class ObjectReference<T>(
    value: T?,
    open val valueChangedListener: ((T?) -> Unit)? = null
) {

    open var value: T? = value
        set(value) {
            field = value

            valueChangedListener?.invoke(value)
        }


    override fun toString(): String {
        return "$value"
    }

}
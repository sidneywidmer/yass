package ch.yass.core.pubsub


interface Action {
    val type: String
        get() = this.javaClass.simpleName
}


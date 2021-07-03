package awais.instagrabber.models

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes

data class Tab(
    @param:DrawableRes val iconResId: Int,
    val title: String,
    val isRemovable: Boolean,

    /**
     * This is the resource id of the root navigation tag of the navigation resource.
     *
     * eg: inside R.navigation.direct_messages_nav_graph, the id of the root tag is R.id.direct_messages_nav_graph.
     *
     * So this field would equal to the value of R.id.direct_messages_nav_graph
     */
    @param:IdRes val navigationRootId: Int,

    /**
     * This is the start destination of the nav graph
     */
    @param:IdRes val startDestinationFragmentId: Int,
)
package awais.instagrabber.repositories.responses.stories

import java.io.Serializable

// https://github.com/austinhuang0131/barinsta/issues/1151
data class StoryAppAttribution(
    val name: String?, // use name instead of app_action_text for button label
    val contentUrl: String?
) : Serializable
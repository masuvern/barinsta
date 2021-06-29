package awais.instagrabber.utils;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.stickers.PollModel;
import awais.instagrabber.models.stickers.QuestionModel;
import awais.instagrabber.models.stickers.QuizModel;
import awais.instagrabber.models.stickers.SliderModel;
import awais.instagrabber.models.stickers.SwipeUpModel;
import awais.instagrabber.repositories.responses.stories.StoryMedia;
import awais.instagrabber.repositories.responses.Caption;
import awais.instagrabber.repositories.responses.FriendshipStatus;
import awais.instagrabber.repositories.responses.ImageVersions2;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.MediaCandidate;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.MediaCandidate;

public final class ResponseBodyUtils {
    private static final String TAG = "ResponseBodyUtils";

    // isI: true if the content was requested from i.instagram.com instead of graphql
    @Nullable
    public static String getHighQualityPost(final JSONArray resources, final boolean isVideo, final boolean isI, final boolean low) {
        try {
            final int resourcesLen = resources.length();

            final String[] sources = new String[resourcesLen];
            int lastResMain = low ? 1000000 : 0, lastIndexMain = -1;
            int lastResBase = low ? 1000000 : 0, lastIndexBase = -1;
            for (int i = 0; i < resourcesLen; ++i) {
                final JSONObject item = resources.getJSONObject(i);
                if (item != null && (!isVideo || item.has(Constants.EXTRAS_PROFILE) || isI)) {
                    sources[i] = item.getString(isI ? "url" : "src");
                    final int currRes = item.getInt(isI ? "width" : "config_width") * item.getInt(isI ? "height" : "config_height");

                    final String profile = isVideo ? item.optString(Constants.EXTRAS_PROFILE) : null;

                    if (!isVideo || "MAIN".equals(profile)) {
                        if (currRes > lastResMain && !low) {
                            lastResMain = currRes;
                            lastIndexMain = i;
                        } else if (currRes < lastResMain && low) {
                            lastResMain = currRes;
                            lastIndexMain = i;
                        }
                    } else {
                        if (currRes > lastResBase && !low) {
                            lastResBase = currRes;
                            lastIndexBase = i;
                        } else if (currRes < lastResBase && low) {
                            lastResBase = currRes;
                            lastIndexBase = i;
                        }
                    }
                }
            }

            if (lastIndexMain >= 0) return sources[lastIndexMain];
            else if (lastIndexBase >= 0) return sources[lastIndexBase];
        } catch (final Exception e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    public static String getHighQualityImage(final JSONObject resources) {
        String src = null;
        try {
            if (resources.has("display_resources"))
                src = getHighQualityPost(resources.getJSONArray("display_resources"), false, false, false);
            else if (resources.has("image_versions2"))
                src = getHighQualityPost(resources.getJSONObject("image_versions2").getJSONArray("candidates"), false, true, false);
            if (src == null) return resources.getString("display_url");
        } catch (final Exception e) {
            Log.e(TAG, "", e);
        }
        return src;
    }

    // the "user" argument can be null, it's used because instagram redacts user details from responses
    public static Media parseGraphQLItem(final JSONObject itemJson, final User backup) throws JSONException {
        if (itemJson == null) {
            return null;
        }
        final JSONObject feedItem = itemJson.has("node") ? itemJson.getJSONObject("node") : itemJson;
        final String mediaType = feedItem.optString("__typename");
        if ("GraphSuggestedUserFeedUnit".equals(mediaType)) return null;

        final boolean isVideo = feedItem.optBoolean("is_video");
        final long videoViews = feedItem.optLong("video_view_count", 0);

        final String displayUrl = feedItem.optString("display_url");
        if (TextUtils.isEmpty(displayUrl)) return null;
        final String resourceUrl;
        if (isVideo && feedItem.has("video_url")) {
            resourceUrl = feedItem.getString("video_url");
        } else {
            resourceUrl = feedItem.has("display_resources") ? ResponseBodyUtils.getHighQualityImage(feedItem) : displayUrl;
        }
        JSONObject tempJsonObject = feedItem.optJSONObject("edge_media_preview_comment");
        final long commentsCount = tempJsonObject != null ? tempJsonObject.optLong("count") : 0;
        tempJsonObject = feedItem.optJSONObject("edge_media_preview_like");
        final long likesCount = tempJsonObject != null ? tempJsonObject.optLong("count") : 0;
        tempJsonObject = feedItem.optJSONObject("edge_media_to_caption");
        final JSONArray captions = tempJsonObject != null ? tempJsonObject.getJSONArray("edges") : null;
        String captionText = null;
        if (captions != null && captions.length() > 0) {
            if ((tempJsonObject = captions.optJSONObject(0)) != null &&
                    (tempJsonObject = tempJsonObject.optJSONObject("node")) != null) {
                captionText = tempJsonObject.getString("text");
            }
        }
        final JSONObject locationJson = feedItem.optJSONObject("location");
        // Log.d(TAG, "location: " + (location == null ? null : location.toString()));
        long locationId = 0;
        String locationName = null;
        if (locationJson != null) {
            locationName = locationJson.optString("name");
            if (locationJson.has("id")) {
                locationId = locationJson.optLong("id");
            } else if (locationJson.has("pk")) {
                locationId = locationJson.optLong("pk");
            }
            // Log.d(TAG, "locationId: " + locationId);
        }
        int height = 0;
        int width = 0;
        final JSONObject dimensions = feedItem.optJSONObject("dimensions");
        if (dimensions != null) {
            height = dimensions.optInt("height");
            width = dimensions.optInt("width");
        }
        String thumbnailUrl = null;
        final List<MediaCandidate> candidates = new ArrayList<MediaCandidate>();
        if (feedItem.has("display_resources") || feedItem.has("thumbnail_resources")) {
            final JSONArray displayResources = feedItem.has("display_resources")
                                               ? feedItem.getJSONArray("display_resources")
                                               : feedItem.getJSONArray("thumbnail_resources");
            for (int i = 0; i < displayResources.length(); i++) {
                final JSONObject displayResource = displayResources.getJSONObject(i);
                candidates.add(new MediaCandidate(
                        displayResource.getInt("config_width"),
                        displayResource.getInt("config_height"),
                        displayResource.getString("src")
                ));
            }
        }
        final ImageVersions2 imageVersions2 = new ImageVersions2(candidates);

        User user = backup;
        long userId = -1;
        if (feedItem.has("owner") && user == null) {
            final JSONObject owner = feedItem.getJSONObject("owner");
            final FriendshipStatus friendshipStatus = new FriendshipStatus(
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false
            );
            userId = owner.optLong(Constants.EXTRAS_ID, -1);
            user = new User(
                    userId,
                    owner.optString(Constants.EXTRAS_USERNAME),
                    owner.optString("full_name"),
                    false,
                    owner.optString("profile_pic_url"),
                    owner.optBoolean("is_verified"));
        }
        final String id = feedItem.getString(Constants.EXTRAS_ID);
        MediaCandidate videoVersion = null;
        if (isVideo) {
            videoVersion = new MediaCandidate(
                    width,
                    height,
                    resourceUrl
            );
        }
        final Caption caption = new Caption(
                userId,
                captionText != null ? captionText : ""
        );

        final boolean isSlider = "GraphSidecar".equals(mediaType) && feedItem.has("edge_sidecar_to_children");
        List<Media> childItems = null;
        if (isSlider) {
            childItems = new ArrayList<>();
            // feedModelBuilder.setItemType(MediaItemType.MEDIA_TYPE_SLIDER);
            final JSONObject sidecar = feedItem.optJSONObject("edge_sidecar_to_children");
            if (sidecar != null) {
                final JSONArray children = sidecar.optJSONArray("edges");
                if (children != null) {
                    // final List<PostChild> sliderItems = getSliderItems(children);
                    // feedModelBuilder.setSliderItems(sliderItems)
                    //                 .setImageHeight(sliderItems.get(0).getHeight())
                    //                 .setImageWidth(sliderItems.get(0).getWidth());
                    for (int i = 0; i < children.length(); i++) {
                        final JSONObject child = children.optJSONObject(i);
                        if (child == null) continue;
                        final Media media = parseGraphQLItem(child, null);
                        media.setSidecarChild(true);
                        childItems.add(media);
                    }
                }
            }
        }
        MediaItemType mediaItemType = MediaItemType.MEDIA_TYPE_IMAGE;
        if (isSlider) {
            mediaItemType = MediaItemType.MEDIA_TYPE_SLIDER;
        } else if (isVideo) {
            mediaItemType = MediaItemType.MEDIA_TYPE_VIDEO;
        }
        final Location location = new Location(
                locationId,
                locationName,
                locationName,
                null,
                null,
                -1,
                -1
        );
        return new Media(
                id,
                id,
                feedItem.optString(Constants.EXTRAS_SHORTCODE),
                feedItem.optLong("taken_at_timestamp", -1),
                user,
                false,
                imageVersions2,
                width,
                height,
                mediaItemType,
                false,
                feedItem.optBoolean("comments_disabled"),
                -1,
                commentsCount,
                likesCount,
                false,
                false,
                isVideo ? Collections.singletonList(videoVersion) : null,
                feedItem.optBoolean("has_audio"),
                feedItem.optDouble("video_duration"),
                videoViews,
                caption,
                false,
                null,
                null,
                childItems,
                location,
                null,
                false,
                false,
                null,
                null,
                null
        );
    }

    public static StoryModel parseStoryItem(final JSONObject data,
                                            final boolean isLocOrHashtag,
                                            final String username) throws JSONException {
        final boolean isVideo = data.has("video_duration");
        final StoryModel model = new StoryModel(data.getString("id"),
                                                data.getJSONObject("image_versions2").getJSONArray("candidates").getJSONObject(0)
                                                    .getString("url"), null,
                                                isVideo ? MediaItemType.MEDIA_TYPE_VIDEO : MediaItemType.MEDIA_TYPE_IMAGE,
                                                data.optLong("taken_at", 0),
                                                isLocOrHashtag ? data.getJSONObject("user").getString("username") : username,
                                                data.getJSONObject("user").getLong("pk"),
                                                data.optBoolean("can_reply"));

        if (data.getJSONObject("image_versions2").getJSONArray("candidates").length() > 1) {
            model.setThumbnail(data.getJSONObject("image_versions2").getJSONArray("candidates").getJSONObject(1)
                                   .getString("url"));
        }

        final JSONArray videoResources = data.optJSONArray("video_versions");
        if (isVideo && videoResources != null)
            model.setVideoUrl(ResponseBodyUtils.getHighQualityPost(videoResources, true, true, false));

        if (data.has("story_feed_media")) {
            model.setTappableShortCode(data.getJSONArray("story_feed_media").getJSONObject(0).optString("media_id"));
        }

        // TODO: this may not be limited to spotify
        if (!data.isNull("story_app_attribution"))
            model.setSpotify(data.getJSONObject("story_app_attribution").optString("content_url").split("\\?")[0]);

        if (data.has("story_polls")) {
            final JSONArray storyPolls = data.optJSONArray("story_polls");
            JSONObject tappableObject = null;
            if (storyPolls != null) {
                tappableObject = storyPolls.getJSONObject(0).optJSONObject("poll_sticker");
            }
            if (tappableObject != null) model.setPoll(new PollModel(
                    String.valueOf(tappableObject.getLong("poll_id")),
                    tappableObject.getString("question"),
                    tappableObject.getJSONArray("tallies").getJSONObject(0).getString("text"),
                    tappableObject.getJSONArray("tallies").getJSONObject(0).getInt("count"),
                    tappableObject.getJSONArray("tallies").getJSONObject(1).getString("text"),
                    tappableObject.getJSONArray("tallies").getJSONObject(1).getInt("count"),
                    tappableObject.optInt("viewer_vote", -1)
            ));
        }
        if (data.has("story_questions")) {
            final JSONObject tappableObject = data.getJSONArray("story_questions").getJSONObject(0)
                                                  .optJSONObject("question_sticker");
            if (tappableObject != null && !tappableObject.getString("question_type").equals("music"))
                model.setQuestion(new QuestionModel(
                        String.valueOf(tappableObject.getLong("question_id")),
                        tappableObject.getString("question")
                ));
        }
        if (data.has("story_quizs")) {
            JSONObject tappableObject = data.getJSONArray("story_quizs").getJSONObject(0).optJSONObject("quiz_sticker");
            if (tappableObject != null) {
                String[] choices = new String[tappableObject.getJSONArray("tallies").length()];
                Long[] counts = new Long[choices.length];
                for (int q = 0; q < choices.length; ++q) {
                    JSONObject tempchoice = tappableObject.getJSONArray("tallies").getJSONObject(q);
                    choices[q] = (q == tappableObject.getInt("correct_answer") ? "*** " : "")
                            + tempchoice.getString("text");
                    counts[q] = tempchoice.getLong("count");
                }
                model.setQuiz(new QuizModel(
                        String.valueOf(tappableObject.getLong("quiz_id")),
                        tappableObject.getString("question"),
                        choices,
                        counts,
                        tappableObject.optInt("viewer_answer", -1)
                ));
            }
        }
        if (data.has("story_cta") && data.has("link_text")) {
            JSONObject tappableObject = data.getJSONArray("story_cta").getJSONObject(0).getJSONArray("links").getJSONObject(0);
            String swipeUpUrl = tappableObject.optString("webUri");
            final String backupSwipeUpUrl = swipeUpUrl;
            if (swipeUpUrl != null && swipeUpUrl.startsWith("https://l.instagram.com/")) {
                swipeUpUrl = Uri.parse(swipeUpUrl).getQueryParameter("u");
            }
            if (swipeUpUrl != null && swipeUpUrl.startsWith("http"))
                model.setSwipeUp(new SwipeUpModel(swipeUpUrl, data.getString("link_text")));
            else if (backupSwipeUpUrl != null && backupSwipeUpUrl.startsWith("http"))
                model.setSwipeUp(new SwipeUpModel(backupSwipeUpUrl, data.getString("link_text")));
        }
        if (data.has("story_sliders")) {
            final JSONObject tappableObject = data.getJSONArray("story_sliders").getJSONObject(0)
                                                  .optJSONObject("slider_sticker");
            if (tappableObject != null)
                model.setSlider(new SliderModel(
                        String.valueOf(tappableObject.getLong("slider_id")),
                        tappableObject.getString("question"),
                        tappableObject.getString("emoji"),
                        tappableObject.getBoolean("viewer_can_vote"),
                        tappableObject.optDouble("slider_vote_average"),
                        tappableObject.getInt("slider_vote_count"),
                        tappableObject.optDouble("viewer_vote")
                ));
        }
        JSONArray hashtags = data.optJSONArray("story_hashtags");
        JSONArray locations = data.optJSONArray("story_locations");
        JSONArray atmarks = data.optJSONArray("reel_mentions");
        String[] mentions = new String[(hashtags == null ? 0 : hashtags.length())
                + (atmarks == null ? 0 : atmarks.length())
                + (locations == null ? 0 : locations.length())];
        if (hashtags != null) {
            for (int h = 0; h < hashtags.length(); ++h) {
                mentions[h] = "#" + hashtags.getJSONObject(h).getJSONObject("hashtag").getString("name");
            }
        }
        if (atmarks != null) {
            for (int h = 0; h < atmarks.length(); ++h) {
                mentions[h + (hashtags == null ? 0 : hashtags.length())] =
                        "@" + atmarks.getJSONObject(h).getJSONObject("user").getString("username");
            }
        }
        if (locations != null) {
            for (int h = 0; h < locations.length(); ++h) {
                mentions[h + (hashtags == null ? 0 : hashtags.length()) + (atmarks == null ? 0 : atmarks.length())] =
                        locations.getJSONObject(h).getJSONObject("location").getString("short_name")
                                + " (" + locations.getJSONObject(h).getJSONObject("location").getLong("pk") + ")";
            }
        }
        if (mentions.length != 0) model.setMentions(mentions);

        return model;
    }

    public static String getThumbUrl(final Object media) {
        return getImageCandidate(media, CandidateType.THUMBNAIL);
    }

    public static String getImageUrl(final Object media) {
        return getImageCandidate(media, CandidateType.DOWNLOAD);
    }

    private static String getImageCandidate(final Object rawMedia, final CandidateType type) {
        final ImageVersions2 imageVersions2;
        final int originalWidth, originalHeight;
        if (rawMedia instanceof StoryMedia) {
            imageVersions2 = ((StoryMedia) rawMedia).getImageVersions2();
            originalWidth = ((StoryMedia) rawMedia).getOriginalWidth();
            originalHeight = ((StoryMedia) rawMedia).getOriginalHeight();
        }
        else if (rawMedia instanceof Media) {
            imageVersions2 = ((Media) rawMedia).getImageVersions2();
            originalWidth = ((Media) rawMedia).getOriginalWidth();
            originalHeight = ((Media) rawMedia).getOriginalHeight();
        }
        else return null;
        if (imageVersions2 == null) return null;
        final List<MediaCandidate> candidates = imageVersions2.getCandidates();
        if (candidates == null || candidates.isEmpty()) return null;
        final boolean isSquare = Integer.compare(originalWidth, originalHeight) == 0;
        final List<MediaCandidate> sortedCandidates = candidates.stream()
                                                                .sorted((c1, c2) -> Integer.compare(c2.getWidth(), c1.getWidth()))
                                                                .collect(Collectors.toList());
        final List<MediaCandidate> filteredCandidates = sortedCandidates.stream()
                                                                        .filter(c ->
                                                                                        c.getWidth() <= originalWidth
                                                                                                && c.getWidth() <= type.getValue()
                                                                                                && (isSquare || Integer
                                                                                                .compare(c.getWidth(), c.getHeight()) != 0)
                                                                        )
                                                                        .collect(Collectors.toList());
        if (filteredCandidates.size() == 0) return sortedCandidates.get(0).getUrl();
        final MediaCandidate candidate = filteredCandidates.get(0);
        if (candidate == null) return null;
        return candidate.getUrl();
    }

    public static String getThumbVideoUrl(final Media media) {
        return getVideoCandidate(media, CandidateType.VIDEO_THUMBNAIL);
    }

    public static String getVideoUrl(final Media media) {
        return getVideoCandidate(media, CandidateType.DOWNLOAD);
    }

    // TODO: merge with getImageCandidate when Kotlin
    private static String getVideoCandidate(final Media media, final CandidateType type) {
        if (media == null) return null;
        final List<MediaCandidate> candidates = media.getVideoVersions();
        if (candidates == null || candidates.isEmpty()) return null;
        final boolean isSquare = Integer.compare(media.getOriginalWidth(), media.getOriginalHeight()) == 0;
        final List<MediaCandidate> sortedCandidates = candidates.stream()
                                                                .sorted((c1, c2) -> Integer.compare(c2.getWidth(), c1.getWidth()))
                                                                .collect(Collectors.toList());
        final List<MediaCandidate> filteredCandidates = sortedCandidates.stream()
                                                                        .filter(c ->
                                                                                        c.getWidth() <= media.getOriginalWidth()
                                                                                                && c.getWidth() <= type.getValue()
                                                                                                && (isSquare || Integer
                                                                                                .compare(c.getWidth(), c.getHeight()) != 0)
                                                                        )
                                                                        .collect(Collectors.toList());
        if (filteredCandidates.size() == 0) return sortedCandidates.get(0).getUrl();
        final MediaCandidate candidate = filteredCandidates.get(0);
        if (candidate == null) return null;
        return candidate.getUrl();
    }

    public static StoryModel parseBroadcastItem(final JSONObject data) throws JSONException {
        final StoryModel model = new StoryModel(data.getString("id"),
                                                data.getString("cover_frame_url"),
                                                data.getString("cover_frame_url"),
                                                MediaItemType.MEDIA_TYPE_LIVE,
                                                data.optLong("published_time", 0),
                                                data.getJSONObject("broadcast_owner").getString("username"),
                                                data.getJSONObject("broadcast_owner").getLong("pk"),
                                                false);
        model.setVideoUrl(data.getString("dash_playback_url"));
        return model;
    }

    private enum CandidateType {
        VIDEO_THUMBNAIL(700),
        THUMBNAIL(1000),
        DOWNLOAD(10000);

        private final int value;

        CandidateType(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}

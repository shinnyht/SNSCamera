package obpro.snscamera;

import android.app.Activity;
import android.os.AsyncTask;

import java.io.File;
import java.util.Map;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.UploadedMedia;
import twitter4j.auth.AccessToken;

/**
 * Created by shinny on 2015/01/09.
 */
public class PictureSender extends AsyncTask<String, Void, String> {
    private Activity cameraActivity;
    private static String consumerKey;
    private static String consumerSecret;
    private static String accessToken;
    private static String accessTokenSecret;

    public PictureSender(Activity cameraActivity, Map<String, String> authInfo) {
        this.cameraActivity = cameraActivity;
        this.consumerKey = authInfo.get("consumer_key");
        this.consumerSecret = authInfo.get("consumer_secret");
        this.accessToken = authInfo.get("access_token");
        this.accessTokenSecret = authInfo.get("access_token_secret");
    }

    private void sendPicture(String text, String path) {
        // send picture to sns
        try {
            final Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(this.consumerKey, this.consumerSecret);
            twitter.setOAuthAccessToken(new AccessToken(this.accessToken, this.accessTokenSecret));

            UploadedMedia image = twitter.uploadMedia(new File(path));
            File imgPath = new File(path);
            if (!imgPath.exists()) {
                return;
            }

            StatusUpdate statusUpdate = new StatusUpdate(text);

            long[] mediaIdArray = new long[1];
            mediaIdArray[0] = image.getMediaId();

            statusUpdate.setMediaIds(mediaIdArray);
            twitter.updateStatus(statusUpdate);

            imgPath.delete();

        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String doInBackground(String... params) {
        String text = params[0];
        String path = params[1];

        sendPicture(text, path);

        return "success";
    }
}

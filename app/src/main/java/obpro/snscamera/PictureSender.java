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
    private static String consumerKey;      // 認証周りの変数
    private static String consumerSecret;
    private static String accessToken;
    private static String accessTokenSecret;

    // コンストラクタ
    public PictureSender(Activity cameraActivity, Map<String, String> authInfo) {
        this.cameraActivity = cameraActivity;
        this.consumerKey = authInfo.get("consumer_key");
        this.consumerSecret = authInfo.get("consumer_secret");
        this.accessToken = authInfo.get("access_token");
        this.accessTokenSecret = authInfo.get("access_token_secret");
    }

    // Twitter へ投稿
    private void sendPicture(String text, String path) {
        try {
            // Twitter インスタンスをnew
            final Twitter twitter = new TwitterFactory().getInstance();
            // Twitter 認証情報をセット
            twitter.setOAuthConsumer(this.consumerKey, this.consumerSecret);
            twitter.setOAuthAccessToken(new AccessToken(this.accessToken, this.accessTokenSecret));

            // 画像ファイルが存在することを確認
            File image = new File(path);
            if (!image.exists()) {
                return;
            }
            UploadedMedia uploadedMedia = twitter.uploadMedia(image);
            long[] mediaIdArray = new long[1];
            mediaIdArray[0] = uploadedMedia.getMediaId();

            // 投稿内容(text とimage)をセット
            StatusUpdate statusUpdate = new StatusUpdate(text);
            statusUpdate.setMediaIds(mediaIdArray);
            // 投稿
            twitter.updateStatus(statusUpdate);

            // 画像ファイルを削除
            image.delete();

        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String doInBackground(String... params) {
        // 第一引数がテキスト文、第二引数が画像ファイルパス
        String text = params[0];
        String path = params[1];

        // Twitter への投稿開始
        sendPicture(text, path);

        return "success";
    }
}

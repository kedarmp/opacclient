package de.geeksfactory.opacclient.networking;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashSet;

import de.geeksfactory.opacclient.objects.CoverHolder;
import de.geeksfactory.opacclient.utils.Base64;
import de.geeksfactory.opacclient.utils.ISBNTools;

public abstract class CoverDownloadTask extends AsyncTask<Void, Integer, CoverHolder> {
    protected static HashSet<String> rejectImages = new HashSet<>();
    protected int width = 0;
    protected int height = 0;

    static {
        rejectImages.add(
                "R0lGODlhOwBLAIAAALy8vf///yH5BAEAAAEALAAAAAA7AEsAAAL/jI+py+0Po5y0" +
                        "2ouz3rz7D2rASJbmiYYGyralGrhyqrbTW4+rGeEhmeA5fCCg4sQgfowLFkLpYTaE" +
                        "O10OIJFCO9KhtYq9Zr+xbpTsDYNh5iR5y2k33/JNPUhHn9UP7T3zd+Cnx0U4xwdn" +
                        "Z3iUx7e0iIcYeDFZJgkJiCnYyKZZ9VRZUTnouDd2WVqYegjqaTHKebUa6SSLKdOJ" +
                        "5GYDY0nVWtvrqxSa61PciytMwbss+uvMjBxNXW19jZ29bHVJu/MNvqmTCK4WhvbF" +
                        "bS65EnPqXiaIJ26Eg/6HVW8+327fHg9kVpBw5xylc6eu3jeBTwh28bewIJh807RZ" +
                        "vIgxo8aNRxw7ZlNXbt04RvT+lXQjL57KciT/nRuY5iW8YzJPQjx5xKVCeCoNurTE" +
                        "0+QukBNZAsu3ECbKnhIBBnwaMWFBVx6rWr2KdUIBADs=");
        rejectImages.add(
                "R0lGODlhOwBLAIABALy8vf///yH+EUNyZWF0ZWQgd2l0aCBHSU1QACH5BAEKAAEA" +
                        "LAAAAAA7AEsAAAJHjI+py+0Po5y02ouz3rz7D4biSJbmiabqyrbuC8fyTNf2jef6z" +
                        "vf+DwwKh8Si8YhMKpfMpvMJjUqn1Kr1is1qt9yu9wsO6woAOw==");
    }

    protected CoverHolder item;
    protected Context context;
    protected HttpClient httpClient;

    public CoverDownloadTask(Context context, CoverHolder item, HttpClient httpClient) {
        this.item = item;
        this.context = context;
        this.httpClient = httpClient;
    }

    @Override
    protected CoverHolder doInBackground(Void... voids) {
        if (item.getCover() != null && item.getCoverBitmap() == null) {
            try {
                if (width == 0 && height == 0) {
                    // Use default
                    float density = context.getResources().getDisplayMetrics().density;
                    width = height = (int) density * 56;
                }

                HttpGet httpget = new HttpGet(ISBNTools.getBestSizeCoverUrl(item.getCover(),
                        width, height));
                HttpResponse response;

                try {
                    response = httpClient.execute(httpget);

                    if (response.getStatusLine().getStatusCode() >= 400) {
                        item.setCover(null);
                    }
                    HttpEntity entity = response.getEntity();
                    byte[] bytes = EntityUtils.toByteArray(entity);
                    if (rejectImages.contains(Base64.encodeBytes(bytes))) {
                        // OPACs like VuFind have a 'cover proxy' that returns a simple GIF with
                        // the text 'no image available' if no cover was found. We don't want to
                        // display this image but the media type,
                        // so we detect it. We do this here
                        // instead of in the API implementation because only this way it can be
                        // done asynchronously.
                        item.setCover(null);
                    } else {
                        if (bytes.length > 64) {
                            item.setCoverBitmap(bytes);
                        } else {
                            // When images embedded from Amazon aren't available, a
                            // 1x1
                            // pixel image is returned (iOPAC)
                            item.setCover(null);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (OutOfMemoryError e) {
                item.setCoverBitmap(null);
                item.setCover(null);
                Log.i("CoverDownloadTask", "OutOfMemoryError");
                return item;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return item;
    }

    protected abstract void onPostExecute(CoverHolder result);
}

package ch.redacted.data.model;

import com.google.gson.annotations.SerializedName;

public class ImgAuth {
  @SerializedName("status") public String status;
  @SerializedName("response") public Response response;

  public static class Response {
    @SerializedName("h") public String h;
    @SerializedName("e") public long e;
    @SerializedName("u") public int u;
  }
}

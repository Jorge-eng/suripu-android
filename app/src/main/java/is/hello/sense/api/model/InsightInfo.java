package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InsightInfo extends ApiResponse {
    @JsonProperty("id")
    private long id;

    @JsonProperty("category")
    private InsightCategory category;

    @JsonProperty("title")
    private String title;

    @JsonProperty("text")
    private String text;

    @JsonProperty("image_url")
    private String imageUrl;


    public long getId() {
        return id;
    }

    public InsightCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getImageUrl() {
        return imageUrl;
    }


    @Override
    public String toString() {
        return "InsightInfo{" +
                "id=" + id +
                ", category=" + category +
                ", title='" + title + '\'' +
                ", text='" + text + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
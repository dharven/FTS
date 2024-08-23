package com.dharven.fts.repository;

import java.util.List;

public class ApiRequest {
    private String model;
    private List<Message> messages;

    public ApiRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    public static class Message {
        private String role;
        private List<Content> content;

        public Message(String role, List<Content> content) {
            this.role = role;
            this.content = content;
        }

        public static class Content {
            private String type;
            private String text;
            private ImageUrl image_url;

            public Content(String type, String text) {
                this.type = type;
                this.text = text;
            }

            public Content(String type, ImageUrl image_url) {
                this.type = type;
                this.image_url = image_url;
            }

            public static class ImageUrl {
                private String url;

                public ImageUrl(String url) {
                    this.url = url;
                }

                public String getUrl() {
                    return url;
                }

                public void setUrl(String url) {
                    this.url = url;
                }
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getText() {
                return text;
            }

            public void setText(String text) {
                this.text = text;
            }

            public ImageUrl getImageUrl() {
                return image_url;
            }

            public void setImageUrl(ImageUrl image_url) {
                this.image_url = image_url;
            }
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public List<Content> getContent() {
            return content;
        }

        public void setContent(List<Content> content) {
            this.content = content;
        }
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}

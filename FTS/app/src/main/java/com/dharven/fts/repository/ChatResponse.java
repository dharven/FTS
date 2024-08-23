package com.dharven.fts.repository;

import java.util.List;

public class ChatResponse {
    private List<Choice> choices;

    public static class Choice {
        private Message message;

        public static class Message {
            private String role;
            private String content;

            // Getters and setters
            public String getRole() {
                return role;
            }

            public void setRole(String role) {
                this.role = role;
            }

            public String getContent() {
                return content;
            }

            public void setContent(String content) {
                this.content = content;
            }
        }

        // Getters and setters
        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

    }

    // Getters and setters
    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }
}

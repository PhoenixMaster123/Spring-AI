package bg.promptcaching.controller;

import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.anthropic.api.AnthropicCacheOptions;
import org.springframework.ai.anthropic.api.AnthropicCacheStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final String systemPrompt;

    public ChatController(ChatClient.Builder builder, @Value("classpath:system-prompt.txt") Resource systemPromptResource) throws IOException {
        this.chatClient = builder.build();
        this.systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    @GetMapping("/")
    public String chat() {
        // setup some chat options
        AnthropicChatOptions chatOptions = AnthropicChatOptions.builder()
                .model(AnthropicApi.ChatModel.CLAUDE_SONNET_4_5)
                .cacheOptions(AnthropicCacheOptions.builder().strategy(AnthropicCacheStrategy.SYSTEM_ONLY).build())
                .build();


        String userPrompt = """
            Generate platform-specific posts for X, Bluesky, and LinkedIn.
            
            Video information:
            - Title: {title}
            - URL: {url}
            - Description: {description}
            """;

        ChatResponse response = chatClient.prompt()
                .options(chatOptions)
                .system(systemPrompt)
                .user(u -> {
                    u.text(userPrompt);
                    u.param("title", "Spring AI Prompt Caching");
                    u.param("url", "https://youtube.com/your-video");
                    u.param("description",
                            "How prompt caching works in Spring AI with Anthropic models.");
                })
                .call()
                .chatResponse();

        AnthropicApi.Usage usage = (AnthropicApi.Usage) response.getMetadata().getUsage().getNativeUsage();

        if (usage != null) {
            System.out.println("Cache creation: " + usage.cacheCreationInputTokens());
            System.out.println("Cache read: " + usage.cacheReadInputTokens());
        }

        return response.getResult().getOutput().getText();
    }
}
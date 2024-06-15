package services;

import lombok.SneakyThrows;
import org.kie.kogito.internal.process.runtime.KogitoWorkItem;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemManager;
import org.kogito.workitem.rest.RestWorkItemHandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class HttpWorkItemHandler implements KogitoWorkItemHandler {

  public static final Logger LOGGER = Logger.getLogger(HttpWorkItemHandler.class.getName());

  private final HttpClient httpClient = HttpClient.newHttpClient();

  private final URI uri;

  public HttpWorkItemHandler(String uri) {
    this.uri = URI.create(uri);
  }

  @SneakyThrows
  @Override
  public void executeWorkItem(KogitoWorkItem kogitoWorkItem, KogitoWorkItemManager kogitoWorkItemManager) {
    LOGGER.info("HTTP request: %s".formatted(uri.toString()));

    final var mapper = new ObjectMapper();
    final var parameters 
      = mapper.writeValueAsString(kogitoWorkItem.getParameters());

    final var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .method("GET", BodyPublishers.ofString(parameters))
        .uri(uri)
        .build();

    final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    final int statusCode = response.statusCode();
    final String responseBody = response.body();
    LOGGER.info("Response status code: %d, response body: %s".formatted(statusCode, responseBody));

    final Map<String, Object> results = new HashMap<>(mapper.readValue(
      responseBody, 
      new TypeReference<Map<String, Object>>() {}
    ));
    kogitoWorkItemManager.completeWorkItem(kogitoWorkItem.getStringId(), results);
  }

  @Override
  public void abortWorkItem(KogitoWorkItem kogitoWorkItem, KogitoWorkItemManager kogitoWorkItemManager) {
    LOGGER.info("Aborting work item: %s".formatted(kogitoWorkItem.getStringId()));
  }
}
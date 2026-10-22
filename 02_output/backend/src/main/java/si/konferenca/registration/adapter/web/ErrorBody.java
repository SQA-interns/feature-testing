package si.konferenca.registration.adapter.web;

import java.util.List;

/** The error body of {@code api-contract.md}: one entry per invalid field. */
public record ErrorBody(List<Item> errors) {

  /** One field error. */
  public record Item(String field, String message) {}

  public static ErrorBody of(String field, String message) {
    return new ErrorBody(List.of(new Item(field, message)));
  }
}

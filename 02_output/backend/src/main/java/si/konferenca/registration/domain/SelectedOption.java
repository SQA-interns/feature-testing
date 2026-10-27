package si.konferenca.registration.domain;

/** An option as accepted with a registration. */
public record SelectedOption(String id, String name, OptionCategory category) {}

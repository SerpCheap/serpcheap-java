package cheap.serp.serpcheap;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** Helper to capture a {@link SerpCheapException} from a thunk. */
final class TestErrors {

  private TestErrors() {}

  static SerpCheapException capture(Runnable thunk) {
    return assertThrows(SerpCheapException.class, thunk::run);
  }
}

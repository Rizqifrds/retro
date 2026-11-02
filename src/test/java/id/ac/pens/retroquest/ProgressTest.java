package id.ac.pens.retroquest;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.pens.retroquest.model.Progress;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProgressTest {
  @ParameterizedTest
  @CsvSource({
    "0,1,1",
    "99,1,1",
    "100,2,1",
    "250,3,1",
    "299,3,1",
    "300,4,2",
    "350,4,2",
    "699,7,2",
    "700,8,3",
    "999,10,3",
    "1000,11,4",
    "2500,26,4"
  })
  void levelAndTierBoundaries(int exp, int level, int tier) {
    var p = new Progress(exp);
    assertEquals(level, p.level());
    assertEquals(tier, p.tier());
  }
}

package id.ac.pens.retroquest.service;

import java.nio.file.Path;

public interface DocumentExtractor {
  String extract(Path path) throws Exception;
}

resource "kafka_topic" "jd_preprocess_text_request" {
  name               = "jd.preprocess.text.request"
  partitions         = 3
  replication_factor = 1
}

resource "kafka_topic" "jd_preprocess_ocr_request" {
  name               = "jd.preprocess.ocr.request"
  partitions         = 3
  replication_factor = 1
}

resource "kafka_topic" "jd_preprocess_url_request" {
  name               = "jd.preprocess.url.request"
  partitions         = 3
  replication_factor = 1
}

resource "kafka_topic" "jd_preprocess_response" {
  name               = "jd.preprocess.response"
  partitions         = 3
  replication_factor = 1
}

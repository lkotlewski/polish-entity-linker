package pl.edu.pw.elka.polishentitylinker.integration.wikidata;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "wikiDataSparqlRestClient", url = "${wikidata.api.url}")
public interface WikiDataSparqlRestClient {

    @GetMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = "text/csv")
    String executeQuery(@RequestParam(name = "query") String query);
}

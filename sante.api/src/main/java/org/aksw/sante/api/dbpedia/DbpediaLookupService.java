package org.aksw.sante.api.dbpedia;

import org.aksw.sante.entity.*;
import org.aksw.sante.smile.core.SmileParams;
import org.apache.lucene.index.IndexReader;
import org.sante.lucene.ResultSet;
import org.sante.lucene.SearchEngine;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DbpediaLookupService {

	public DbpediaDocumentCollection lookupDbpedia(Integer maxHits, String searchQuery, Set<String> searchClasses) throws IOException {
		var docs = new DbpediaDocumentCollection();
		Path indexPath = (new File(SmileParams.getInstance().indexPath)).toPath();
		try (IndexReader reader = SearchEngine.newReader(indexPath)) {
			SearchEngine searchEngine = new SearchEngine(reader);
			ResultSet<Entity> results = searchEngine.search(
					searchQuery,
					0,
					maxHits,
					null,
					searchClasses,
					null,
					true
			);
			while (results.hasNext()) {
				var doc = new DbpediaDocument();

				Entity entity = results.next();

				var resource = entity.getURI();
				doc.setResource(Collections.singletonList(resource));

				var score = Float.toString(results.score());
				doc.setScore(Collections.singletonList(score));

				List<String> label = entity.getLabels().stream().map(Literal::getValue).collect(Collectors.toList());
				doc.setLabel(label);

				List<Property> typeProperties = entity.getProperties("http://www.w3.org/2000/01/rdf-schema#type");
				if (typeProperties != null) {
					List<String> type = typeProperties.stream()
							.map(typeProperty -> typeProperty.getObject().getURI())
							.collect(Collectors.toList());
					doc.setType(type);
					List<String> typeName = typeProperties.stream()
							.flatMap(typeProperty -> ((URIObject) typeProperty.getObject()).getLabels()
									.stream().map(Literal::getValue))
							.collect(Collectors.toList());
					doc.setTypeName(typeName);
				}

				List<Property> commentProperties = entity.getProperties("http://www.w3.org/1999/02/22-rdf-syntax-ns#comment");
				if (commentProperties != null) {
					List<String> comment = commentProperties.stream()
							.map(commentProperty -> ((LiteralObject) commentProperty.getObject()).getValue())
							.collect(Collectors.toList());
					doc.setComment(comment);
				}

				docs.addToDocs(doc);
			}
		} catch (Exception e) {}
		return docs;
	}
}
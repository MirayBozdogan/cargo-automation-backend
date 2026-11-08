package com.example.staj1.repository;

import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TurkeyLocationsPersistenceTests {
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;

    void importLocations() {
        new ResourceDatabasePopulator(new ClassPathResource("db/reference/turkey-locations.sql")).execute(dataSource);
    }
    long count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class); }
    Map<Integer, String> snapshot(String table) {
        return jdbc.query("SELECT id, to_jsonb(t)::text AS body FROM " + table + " t", result -> {
            Map<Integer,String> rows = new HashMap<>();
            while (result.next()) rows.put(result.getInt("id"),result.getString("body"));
            return rows;
        });
    }
    @Test void existingIdsAddressesShipmentsAndPricesSurviveRepeatedImport() {
        // Deliberately use IDs different from source plate codes.
        jdbc.update("INSERT INTO cities(id,name) VALUES (9001,'İstanbul'),(9002,'Ankara'),(9003,'İzmir')");
        jdbc.update("INSERT INTO districts(id,name,city_id) VALUES " +
                "(9101,'Kadıköy',9001),(9102,'Üsküdar',9001),(9103,'Çankaya',9002)," +
                "(9104,'Keçiören',9002),(9105,'Konak',9003),(9106,'Karşıyaka',9003)");
        jdbc.update("INSERT INTO addresses(id,city_id,district_id,neighborhood,building_no) VALUES (9201,9001,9101,'Kayıtlı mahalle',7)");
        jdbc.update("INSERT INTO shipments(id,barcode,status,sender_city,sender_district,receiver_address_text,deleted) " +
                "VALUES (9301,'LOCATIONS001','CREATED','İstanbul','Kadıköy','Kayıtlı adres',false)");
        jdbc.update("INSERT INTO prices(id,min_desi,max_desi,price) VALUES (9401,0,1,123)");
        var tables = List.of("cities","districts","addresses","shipments","prices");
        Map<String,Map<Integer,String>> before = new HashMap<>();
        for (String table : tables) before.put(table,snapshot(table));
        importLocations();
        assertEquals(81,count("cities")); assertEquals(973,count("districts"));
        for (String table : tables) {
            var after = snapshot(table);
            before.get(table).forEach((id,body) -> assertEquals(body,after.get(id),table+" #"+id));
        }
        Map<String,Map<Integer,String>> first = new HashMap<>();
        for (String table : tables) first.put(table,snapshot(table));
        importLocations();
        for (String table : tables) assertEquals(first.get(table),snapshot(table),table);
        assertEquals(9001,jdbc.queryForObject("SELECT city_id FROM districts WHERE id=9101",Integer.class));
    }
    @Test void emptyDatabaseGetsAllLocationsIncludingEachCentralDistrictWithCorrectParents() {
        importLocations();
        assertEquals(81,count("cities")); assertEquals(973,count("districts"));
        assertEquals(51,jdbc.queryForObject("SELECT count(*) FROM districts WHERE name='Merkez'",Long.class));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM districts d LEFT JOIN cities c ON c.id=d.city_id WHERE c.id IS NULL",Long.class));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM (SELECT city_id,name FROM districts GROUP BY city_id,name HAVING count(*)>1) duplicates",Long.class));
        for (var expected : Map.of("İstanbul",39,"Ankara",25,"İzmir",30,"Bursa",17).entrySet()) {
            assertEquals(expected.getValue().longValue(),jdbc.queryForObject(
                    "SELECT count(*) FROM districts d JOIN cities c ON c.id=d.city_id WHERE c.name=?",Long.class,expected.getKey()));
        }
    }
}

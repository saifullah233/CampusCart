package com.campuscart.location;

import com.campuscart.college.domain.College;
import com.campuscart.location.domain.City;
import com.campuscart.support.AbstractMySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class LocationPublicApiIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private UUID activeCity1Id;
    private UUID activeCity2Id;
    private UUID inactiveCityId;

    private UUID activeCollege1Id;
    private UUID activeCollege2Id;
    private UUID inactiveCollegeId;

    @BeforeEach
    void setUpTestData() {
        // Seed Cities
        City activeCity1 = new City("Boston", "MA");
        City activeCity2 = new City("Austin", "TX");
        City inactiveCity = new City("Chicago", "IL");
        inactiveCity.deactivate();

        entityManager.persist(activeCity1);
        entityManager.persist(activeCity2);
        entityManager.persist(inactiveCity);

        activeCity1Id = activeCity1.getId();
        activeCity2Id = activeCity2.getId();
        inactiveCityId = inactiveCity.getId();

        // Seed Colleges for activeCity1
        College activeCollege1 = new College("Harvard University", activeCity1);
        College activeCollege2 = new College("Boston University", activeCity1);
        College inactiveCollege = new College("MIT", activeCity1);
        inactiveCollege.deactivate();

        entityManager.persist(activeCollege1);
        entityManager.persist(activeCollege2);
        entityManager.persist(inactiveCollege);

        activeCollege1Id = activeCollege1.getId();
        activeCollege2Id = activeCollege2.getId();
        inactiveCollegeId = inactiveCollege.getId();

        // Seed College for activeCity2
        College austinCollege = new College("University of Texas", activeCity2);
        entityManager.persist(austinCollege);

        entityManager.flush();
    }

    @Test
    void listCitiesReturnsOnlyActiveCitiesSortedAlphabeticallyWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/cities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(8)))
                // Austin (A) should be first, Boston (B) second. Chicago (inactive) omitted.
                .andExpect(jsonPath("$.data[0].name").value("Austin"))
                .andExpect(jsonPath("$.data[1].name").value("Boston"));
    }

    @Test
    void listCollegesReturnsOnlyActiveCollegesForCitySortedAlphabeticallyWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/colleges")
                        .param("cityId", activeCity1Id.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                // Boston University (B) should be first, Harvard (H) second. MIT (inactive) omitted.
                .andExpect(jsonPath("$.data[0].name").value("Boston University"))
                .andExpect(jsonPath("$.data[1].name").value("Harvard University"));
    }

    @Test
    void listCollegesReturns404ForInactiveOrNonExistentCity() throws Exception {
        // Non-existent city ID
        mockMvc.perform(get("/api/v1/colleges")
                        .param("cityId", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        // Inactive city ID
        mockMvc.perform(get("/api/v1/colleges")
                        .param("cityId", inactiveCityId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void listCollegesReturns400ForMissingOrMalformedCityId() throws Exception {
        // Missing parameter
        mockMvc.perform(get("/api/v1/colleges")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Malformed UUID parameter
        mockMvc.perform(get("/api/v1/colleges")
                        .param("cityId", "not-a-valid-uuid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void detectCollegeByEmailDomainResolvesCorrectCollegeAndCity() throws Exception {
        // Seed an email domain for activeCollege1 (Harvard in Boston)
        College college = entityManager.find(College.class, activeCollege1Id);
        com.campuscart.college.domain.CollegeEmailDomain domain =
                new com.campuscart.college.domain.CollegeEmailDomain("harvard.edu", college);
        entityManager.persist(domain);
        entityManager.flush();

        // 4 & 5. Known domain resolves to correct institution and city
        mockMvc.perform(get("/api/v1/colleges/by-email-domain/{domain}", "harvard.edu")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.collegeId").value(activeCollege1Id.toString()))
                .andExpect(jsonPath("$.data.collegeName").value("Harvard University"))
                .andExpect(jsonPath("$.data.cityId").value(activeCity1Id.toString()))
                .andExpect(jsonPath("$.data.cityName").value("Boston"));

        // 7. Uppercase domain works
        mockMvc.perform(get("/api/v1/colleges/by-email-domain/{domain}", "HARVARD.EDU")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.collegeId").value(activeCollege1Id.toString()));

        // 6. Unknown domain returns null / no college
        mockMvc.perform(get("/api/v1/colleges/by-email-domain/{domain}", "unknown-college.edu")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        // 8. Invalid domain prefix/suffix does not falsely match
        mockMvc.perform(get("/api/v1/colleges/by-email-domain/{domain}", "evilharvard.edu")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}

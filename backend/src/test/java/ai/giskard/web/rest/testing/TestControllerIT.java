package ai.giskard.web.rest.testing;

import ai.giskard.IntegrationTest;
import ai.giskard.domain.Project;
import ai.giskard.domain.ml.Dataset;
import ai.giskard.repository.ProjectRepository;
import ai.giskard.repository.ml.DatasetRepository;
import ai.giskard.repository.ml.ModelRepository;
import ai.giskard.security.AuthoritiesConstants;
import ai.giskard.service.InitService;
import ai.giskard.service.TestSuiteService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@IntegrationTest
@WithMockUser(username = "admin", authorities = AuthoritiesConstants.ADMIN)
class TestControllerIT {

    @Autowired
    private MockMvc restUserMockMvc;

    @Autowired
    private TestSuiteService testSuiteService;
    @Autowired
    private DatasetRepository datasetRepository;
    @Autowired
    private ModelRepository modelRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private InitService initService;

    @Test
    @Transactional
    void testDatasetMetadata() throws Exception {
        Project project = projectRepository.getOneByName(initService.getProjectByCreatorLogin("admin"));
        Optional<Dataset> dataset = project.getDatasets().stream().findFirst();
        Assertions.assertFalse(dataset.isEmpty(), "demo dataset not found");
        restUserMockMvc.perform(get(String.format("/api/v2/project/%s/datasets/%s", dataset.get().getProject().getKey(), dataset.get().getId())).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.target").isString())
            .andExpect(jsonPath("$.columnMeanings").isNotEmpty());
    }
}

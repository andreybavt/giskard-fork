package ai.giskard.web.rest.controllers;

import ai.giskard.domain.Project;
import ai.giskard.repository.ProjectRepository;
import ai.giskard.security.PermissionEvaluator;
import ai.giskard.service.FileLocationService;
import ai.giskard.service.GiskardRuntimeException;
import ai.giskard.service.ProjectService;
import ai.giskard.web.dto.mapper.GiskardMapper;
import ai.giskard.web.dto.ml.ProjectDTO;
import ai.giskard.web.dto.ml.ProjectPostDTO;
import ai.giskard.web.rest.errors.NotInDatabaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Controller for the {@link Project} resource
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class ProjectController {
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final GiskardMapper giskardMapper;
    private final PermissionEvaluator permissionEvaluator;
    private final FileLocationService locationService;

    /**
     * Retrieve the list of projects accessible by the authenticated user
     *
     * @return List of projects
     */
    @GetMapping("projects")
    public List<ProjectDTO> list() throws NotInDatabaseException {
        List<Project> projects = projectService.list();
        return giskardMapper.projectsToProjectDTOs(projects);
    }

    /**
     * Update the project with the specified project
     *
     * @param projectDTO project updated to save
     * @param id         id of the existing project
     * @return updated project
     */
    @PreAuthorize("@permissionEvaluator.canWriteProject( #id)")
    @PutMapping(value = "/project/{id}")
    public ProjectDTO updateProject(@RequestBody ProjectPostDTO projectDTO, @PathVariable("id") Long id) {
        Project updatedProject = this.projectService.update(id, projectDTO);
        return giskardMapper.projectToProjectDTO(updatedProject);
    }

    /**
     * Create new project
     *
     * @param projectPostDTO project to save
     * @return created project
     */
    @PostMapping(value = "/project")
    @PreAuthorize("@permissionEvaluator.canWrite()")
    public ProjectDTO create(@RequestBody @Valid ProjectPostDTO projectPostDTO, @AuthenticationPrincipal final UserDetails userDetails) {
        Project project = this.giskardMapper.projectPostDTOToProject(projectPostDTO);
        Project savedProject = this.projectService.create(project, userDetails.getUsername());
        return giskardMapper.projectToProjectDTO(savedProject);
    }

    /**
     * Show the specified project
     *
     * @param id id of the project
     * @return created project
     */
    @GetMapping(value = "project")
    @Transactional
    public ProjectDTO getProject(@RequestParam(value = "id", required = false) Long id, @RequestParam(value = "key", required = false) String key) {
        Project project;
        if (id != null) {
            project = this.projectRepository.getById(id);
        } else if (key != null) {
            project = this.projectRepository.getOneByKey(key);
        } else {
            throw new IllegalArgumentException("Either project id or project key should be specified");
        }
        permissionEvaluator.validateCanReadProject(project.getId());
        return giskardMapper.projectToProjectDTO(project);
    }

    /**
     * Delete project
     *
     * @param id project's id to delete
     * @return msg Deleted if deleted
     */
    @DeleteMapping(value = "/project/{id}")
    @PreAuthorize("@permissionEvaluator.canWriteProject( #id)")
    public void delete(@PathVariable("id") Long id) {
        this.projectService.delete(id);
    }

    /**
     * Remove user from project's guestlist
     *
     * @param id     project's id
     * @param userId user's id
     * @return updated project
     */
    @DeleteMapping(value = "/project/{id}/guests/{userId}")
    @PreAuthorize("@permissionEvaluator.canWriteProject( #id)")
    @Transactional
    public ProjectDTO uninvite(@PathVariable("id") Long id, @PathVariable("userId") Long userId) {
        Project project = this.projectService.uninvite(id, userId);
        return giskardMapper.projectToProjectDTO(project);
    }

    /**
     * Add user to project's guestlist
     *
     * @param id     project's id
     * @param userId user's id
     * @return project updated
     */
    @PreAuthorize("@permissionEvaluator.canWriteProject( #id)")
    @PutMapping(value = "/project/{id}/guests/{userId}")
    @Transactional
    public ProjectDTO invite(@PathVariable("id") Long id, @PathVariable("userId") Long userId) {
        Project project = this.projectService.invite(id, userId);
        return giskardMapper.projectToProjectDTO(project);
    }

    @GetMapping(
        value = "/project/{id}/export",
        produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    @Transactional
    public @ResponseBody byte[] exportProject(@PathVariable("id") Long id) throws IOException {
        return this.projectService.export(id);
    }

    @PostMapping(value = "/project/import/{timestampDirectory}/{projectKey}/prepare", consumes = "multipart/form-data")
    public PrepareImportProjectDTO PrepareImport(@RequestParam("file") MultipartFile zipFile, @PathVariable("timestampDirectory") @NotNull String timestampDirectory, @PathVariable("projectKey") String projectKey, @AuthenticationPrincipal final UserDetails userDetails) throws IOException {
        permissionEvaluator.canWrite();
        Path pathToTimestampDirectory;
        pathToTimestampDirectory = projectService.unzip(timestampDirectory, zipFile);
        try {
            return projectService.prepareImport(pathToTimestampDirectory, projectKey, userDetails.getUsername());
        } catch (IOException e ) {
            FileSystemUtils.deleteRecursively(pathToTimestampDirectory);
            throw new GiskardRuntimeException("Error while preparing your project import", e);
        }
    }

    @PostMapping(value = "/project/import/{timestampDirectory}/{projectKey}")
    @Transactional
    public ProjectDTO importProject(@RequestBody Map<String, String> mappedUsers, @PathVariable("projectKey") @NotNull String projectKey, @PathVariable("timestampDirectory") @NotNull String timestampDirectory, @AuthenticationPrincipal final UserDetails userDetails) throws IOException {
        try {
            return projectService.importProject(mappedUsers, timestampDirectory, projectKey, userDetails.getUsername());
        } catch (IOException e){
            throw new GiskardRuntimeException("Error while importing the project", e);
        }finally {
            FileSystemUtils.deleteRecursively(locationService.resolvedTmpPath());
        }
    }
}

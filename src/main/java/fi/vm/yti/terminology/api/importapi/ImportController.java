package fi.vm.yti.terminology.api.importapi;

import java.util.UUID;

import fi.vm.yti.terminology.api.exception.ExcelParseException;
import fi.vm.yti.terminology.api.importapi.excel.ExcelImportResponseDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import fi.vm.yti.terminology.api.frontend.FrontendController;
import fi.vm.yti.terminology.api.frontend.FrontendTermedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping({"/api/v1/import", "/api/v1/frontend/import"})
@Tag(name = "Import-Export")
public class ImportController {

    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);
    private final ImportService importService;

    public ImportController(FrontendTermedService termedService,
                            ImportService importService) {
        this.importService = importService;
    }

    @Operation(summary = "Initiate NTRF import job", description = "Start the procedure to import concepts from a NTRF (XML) document")
    @ApiResponse(
        responseCode = "200",
        description = "If import process started successfully then job token is returned as JSON",
        content = { @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ImportService.ImportResponse.class)) })
    @PostMapping(path = "ntrf/{terminology}", consumes = MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<String> importTerms(@Parameter(description = "The ID of the terminology to import concepts to") @PathVariable("terminology") UUID terminologyId,
                                       @Parameter(required = true, description = "The NTRF (XML) document containing the concepts to be imported", style = ParameterStyle.FORM)
                                       @RequestPart(value = "file") MultipartFile file) {
        terminologyId = UUID.fromString(HtmlUtils.htmlEscape(terminologyId.toString()));
        return importService.handleNtrfDocumentAsync("ntrf", terminologyId, file);
    }

    @Operation(summary = "Initiate Excel import job", description = "Start the procedure to import concepts from Excel file")
    @ApiResponse(
            responseCode = "200",
            description = "If import process started successfully then job token is returned as JSON",
            content = { @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ImportService.ImportResponse.class)) })
    @PostMapping(path = "excel", consumes = MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ExcelImportResponseDTO> importExcel(@RequestPart(value = "file") MultipartFile file)  {

        try {
            UUID jobToken = importService.handleExcelImport(file.getInputStream());
            return ResponseEntity.ok(new ExcelImportResponseDTO(jobToken, "SUCCESS"));
        } catch (ExcelParseException e) {
            logger.error("Error parsing excel", e);
            return ResponseEntity
                    .badRequest()
                    .body(
                        new ExcelImportResponseDTO(
                                null,
                                e.getReason(),
                                new ExcelImportResponseDTO.ErrorDetails(e.getSheet(), e.getRowNumber(), e.getColumn())
                            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Operation(summary = "Initiate simple Excel import job", description = "Start the procedure to import concepts from Excel file")
    @ApiResponse(
            responseCode = "200",
            description = "If import process started successfully then job token is returned as JSON",
            content = {@Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ImportService.ImportResponse.class))})
    @PostMapping(path = "simpleExcel/{terminology}", consumes = MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ExcelImportResponseDTO> importSimpleExcel(@Parameter(description = "The ID of the terminology to import concepts to")
                                                             @PathVariable("terminology") UUID terminologyId,
                                                             @RequestPart(value = "file") MultipartFile file) {
        try {
            UUID jobId = importService.handleSimpleExcelImport(terminologyId, file.getInputStream());
            return ResponseEntity.ok(new ExcelImportResponseDTO(jobId, "SUCCESS"));
        } catch (ExcelParseException e) {
            logger.error("Error parsing excel", e);
            return ResponseEntity
                    .badRequest()
                    .body(
                            new ExcelImportResponseDTO(
                                    null,
                                    e.getReason(),
                                    new ExcelImportResponseDTO.ErrorDetails(e.getSheet(), e.getRowNumber(), e.getColumn())
                            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Operation(summary = "Poll status of import job", description = "Get the current status of previously initiated import job")
    @ApiResponse(
        responseCode = "200",
        description = "Returns status object for previously initiated import job",
        content = { @Content(schema = @Schema(implementation = ImportStatusResponse.class)) })
    @GetMapping(path = "/status/{jobtoken}", produces = APPLICATION_JSON_VALUE)
    ResponseEntity<String> getStatus(@Parameter(description = "The job token returned by import request") @PathVariable("jobtoken") UUID id,
                                     @Parameter(description = "Set to true to fetch full status including messages; useful for finished jobs") @RequestParam(name = "full", required = false, defaultValue = "false") boolean full) {
        return importService.getStatus(id, full);
    }
}

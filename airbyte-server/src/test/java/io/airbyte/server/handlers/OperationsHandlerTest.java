/*
 * MIT License
 *
 * Copyright (c) 2020 Airbyte
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.airbyte.server.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.airbyte.api.model.ConnectionIdRequestBody;
import io.airbyte.api.model.OperationCreate;
import io.airbyte.api.model.OperationIdRequestBody;
import io.airbyte.api.model.OperationRead;
import io.airbyte.api.model.OperationReadList;
import io.airbyte.api.model.OperationUpdate;
import io.airbyte.api.model.OperatorConfiguration;
import io.airbyte.api.model.OperatorDbt;
import io.airbyte.api.model.OperatorNormalization;
import io.airbyte.api.model.OperatorNormalization.OptionEnum;
import io.airbyte.api.model.OperatorType;
import io.airbyte.commons.enums.Enums;
import io.airbyte.config.OperatorNormalization.Option;
import io.airbyte.config.StandardSync;
import io.airbyte.config.StandardSyncOperation;
import io.airbyte.config.persistence.ConfigNotFoundException;
import io.airbyte.config.persistence.ConfigRepository;
import io.airbyte.validation.json.JsonValidationException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OperationsHandlerTest {

  private ConfigRepository configRepository;
  private Supplier<UUID> uuidGenerator;

  private OperationsHandler operationsHandler;
  private StandardSyncOperation standardSyncOperation;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() throws IOException {
    configRepository = mock(ConfigRepository.class);
    uuidGenerator = mock(Supplier.class);

    operationsHandler = new OperationsHandler(configRepository, uuidGenerator);
    standardSyncOperation = new StandardSyncOperation()
        .withOperationId(UUID.randomUUID())
        .withName("presto to hudi")
        .withOperatorType(io.airbyte.config.StandardSyncOperation.OperatorType.NORMALIZATION)
        .withOperatorNormalization(new io.airbyte.config.OperatorNormalization().withOption(Option.BASIC))
        .withOperatorDbt(null)
        .withTombstone(false);
  }

  @Test
  void testCreateOperation() throws JsonValidationException, ConfigNotFoundException, IOException {
    when(uuidGenerator.get()).thenReturn(standardSyncOperation.getOperationId());

    when(configRepository.getStandardSyncOperation(standardSyncOperation.getOperationId())).thenReturn(standardSyncOperation);

    final OperationCreate operationCreate = new OperationCreate()
        .name(standardSyncOperation.getName())
        .operatorConfiguration(new OperatorConfiguration()
            .operatorType(OperatorType.NORMALIZATION)
            .normalization(new OperatorNormalization().option(OptionEnum.BASIC)));

    final OperationRead actualOperationRead = operationsHandler.createOperation(operationCreate);

    final OperationRead expectedOperationRead = new OperationRead()
        .operationId(standardSyncOperation.getOperationId())
        .name(standardSyncOperation.getName())
        .operatorConfiguration(new OperatorConfiguration()
            .operatorType(OperatorType.NORMALIZATION)
            .normalization(new OperatorNormalization().option(OptionEnum.BASIC)));

    assertEquals(expectedOperationRead, actualOperationRead);

    verify(configRepository).writeStandardSyncOperation(standardSyncOperation);
  }

  @Test
  void testUpdateOperation() throws JsonValidationException, ConfigNotFoundException, IOException {
    final OperationUpdate operationUpdate = new OperationUpdate()
        .operationId(standardSyncOperation.getOperationId())
        .name(standardSyncOperation.getName())
        .operatorConfiguration(new OperatorConfiguration()
            .operatorType(OperatorType.DBT)
            .dbt(new OperatorDbt()
                .gitRepoUrl("git_repo_url")
                .gitRepoBranch("git_repo_branch")
                .dockerImage("docker")
                .dbtArguments("--full-refresh")));

    final StandardSyncOperation updatedStandardSyncOperation = new StandardSyncOperation()
        .withOperationId(standardSyncOperation.getOperationId())
        .withName(standardSyncOperation.getName())
        .withOperatorType(io.airbyte.config.StandardSyncOperation.OperatorType.DBT)
        .withOperatorDbt(new io.airbyte.config.OperatorDbt()
            .withGitRepoUrl("git_repo_url")
            .withGitRepoBranch("git_repo_branch")
            .withDockerImage("docker")
            .withDbtArguments("--full-refresh"))
        .withOperatorNormalization(null)
        .withTombstone(false);

    when(configRepository.getStandardSyncOperation(standardSyncOperation.getOperationId())).thenReturn(standardSyncOperation)
        .thenReturn(updatedStandardSyncOperation);

    final OperationRead actualOperationRead = operationsHandler.updateOperation(operationUpdate);

    final OperationRead expectedOperationRead = new OperationRead()
        .operationId(standardSyncOperation.getOperationId())
        .name(standardSyncOperation.getName())
        .operatorConfiguration(new OperatorConfiguration()
            .operatorType(OperatorType.DBT)
            .dbt(new OperatorDbt()
                .gitRepoUrl("git_repo_url")
                .gitRepoBranch("git_repo_branch")
                .dockerImage("docker")
                .dbtArguments("--full-refresh")));

    assertEquals(expectedOperationRead, actualOperationRead);

    verify(configRepository).writeStandardSyncOperation(updatedStandardSyncOperation);
  }

  @Test
  void testGetOperation() throws JsonValidationException, ConfigNotFoundException, IOException {
    when(configRepository.getStandardSyncOperation(standardSyncOperation.getOperationId())).thenReturn(standardSyncOperation);

    final OperationIdRequestBody operationIdRequestBody = new OperationIdRequestBody().operationId(standardSyncOperation.getOperationId());
    final OperationRead actualOperationRead = operationsHandler.getOperation(operationIdRequestBody);

    final OperationRead expectedOperationRead = generateOperationRead();

    assertEquals(expectedOperationRead, actualOperationRead);
  }

  private OperationRead generateOperationRead() {
    return new OperationRead()
        .operationId(standardSyncOperation.getOperationId())
        .name(standardSyncOperation.getName())
        .operatorConfiguration(new OperatorConfiguration()
            .operatorType(OperatorType.NORMALIZATION)
            .normalization(new OperatorNormalization().option(OptionEnum.BASIC)));
  }

  @Test
  void testListOperationsForConnection() throws JsonValidationException, ConfigNotFoundException, IOException {
    UUID connectionId = UUID.randomUUID();

    when(configRepository.getStandardSync(connectionId))
        .thenReturn(new StandardSync()
            .withOperationIds(List.of(standardSyncOperation.getOperationId())));

    when(configRepository.getStandardSyncOperation(standardSyncOperation.getOperationId()))
        .thenReturn(standardSyncOperation);

    when(configRepository.listStandardSyncOperations())
        .thenReturn(List.of(standardSyncOperation));

    final ConnectionIdRequestBody connectionIdRequestBody = new ConnectionIdRequestBody().connectionId(connectionId);
    final OperationReadList actualOperationReadList = operationsHandler.listOperationsForConnection(connectionIdRequestBody);

    assertEquals(generateOperationRead(), actualOperationReadList.getOperations().get(0));
  }

  @Test
  void testDeleteOperation() throws JsonValidationException, IOException, ConfigNotFoundException {
    final OperationIdRequestBody operationIdRequestBody = new OperationIdRequestBody().operationId(standardSyncOperation.getOperationId());

    final OperationsHandler spiedOperationsHandler = spy(operationsHandler);
    when(configRepository.getStandardSyncOperation(standardSyncOperation.getOperationId())).thenReturn(standardSyncOperation);

    spiedOperationsHandler.deleteOperation(operationIdRequestBody);

    verify(configRepository).writeStandardSyncOperation(standardSyncOperation.withTombstone(true));
  }

  @Test
  void testEnumConversion() {
    assertTrue(Enums.isCompatible(io.airbyte.api.model.OperatorType.class, io.airbyte.config.StandardSyncOperation.OperatorType.class));
    assertTrue(Enums.isCompatible(io.airbyte.api.model.OperatorNormalization.OptionEnum.class, io.airbyte.config.OperatorNormalization.Option.class));
  }

}

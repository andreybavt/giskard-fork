<template>
  <div>
    <v-container fluid>
      <v-row>
        <v-col :align="'right'">
          <v-btn small tile color="primary" class="mx-1" @click="createTestSuite()">
            <v-icon left>add</v-icon>
            create test suite
          </v-btn>
        </v-col>
      </v-row>
      <v-data-table
          class="row-pointer"
          :items="testSuites"
          :headers="tableHeaders"
          @click:row="openTestSuite"
      >
      </v-data-table>
    </v-container>
  </div>
</template>

<script lang="ts">

import {Prop, Vue} from "vue-property-decorator";
import Component from "vue-class-component";
import TestSuiteCreateModal from "@/views/main/project/modals/TestSuiteCreateModal.vue";
import {api} from "@/api";
import {DatasetDTO, ModelDTO, TestSuiteDTO} from '@/generated-sources';

@Component({
  components: {TestSuiteCreateModal}
})
export default class TestSuites extends Vue {
  @Prop({required: true}) projectId!: number;

  testSuites: Array<TestSuiteDTO> = []

  private static getProjectFileName(obj: ModelDTO | DatasetDTO) {
    return obj ? (obj.name || obj.fileName) : "";
  }

  get tableHeaders() {
    return [
      {
        text: "Name",
        sortable: true,
        value: "name",
        align: "left"
      },
      {
        text: "Model",
        sortable: true,
        value: "model.name",
        align: "left"
      },
      {
        text: "Reference dataset",
        sortable: true,
        value: "referenceDataset.name",
        align: "left"
      },
      {
        text: "Actual dataset",
        sortable: true,
        value: "actualDataset.name",
        align: "left"
      },
      {
        text: "id",
        sortable: false,
        value: "id",
        align: "left"
      }
    ];
  }

  public openTestSuite(suite) {
    this.$router.push({name: 'suite-details', params: {suiteId: suite.id}})
  }

  public async createTestSuite() {
    const newTestSuite = await this.$dialog.showAndWait(TestSuiteCreateModal, {
      width: 800, projectId: this.projectId, scrollable: true
    });
    await this.$router.push({
      name: 'suite-details', params: {
        projectId: this.projectId.toString(),
        suiteId: newTestSuite.id
      }
    });
  }

  public async activated() {
    await this.init();
  }

  public async mounted() {
    await this.init();
  }

  private async init() {
    this.testSuites = (await api.getTestSuites(this.projectId)).map((ts: TestSuiteDTO) => {
      ts.model.name = TestSuites.getProjectFileName(ts.model);
      if (ts.referenceDataset) {
        ts.referenceDataset.name = TestSuites.getProjectFileName(ts.referenceDataset);
      }
      if (ts.actualDataset) {
        ts.actualDataset.name = TestSuites.getProjectFileName(ts.actualDataset);
      }
      return ts;
    });
  }
}
</script>
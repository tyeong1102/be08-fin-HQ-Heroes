<template>
    <div class="card">
        <div class="font-semibold text-xl mb-4">전체 사원 목록</div>
        <DataTable
            :value="filteredEmployees"
            :paginator="true"
            :rows="10"
            removableSort
            dataKey="employeeNo"
            :rowHover="true"
            v-model:filters="filters"
            filterDisplay="menu"
            :filters="filters"
            selectionMode="single"
            :globalFilterFields="['employeeName', 'deptName', 'jobRoleName', 'teamName', 'positionName', 'employeeId', 'joinDate']"
            showGridlines
            @row-click="showEmployeeDetails"
            :metaKeySelection="false"
            @rowSelect="onRowSelect"
            @rowUnselect="onRowUnselect"
        >
            <template #header>
                <div class="flex justify-between items-center">
                    <div class="flex items-center gap-2">
                        <Dropdown v-model="selectedDepartment" :options="departments" optionLabel="deptName" placeholder="부서를 선택하세요" @change="handleDepartmentChange" class="mr-2" />
                        <Dropdown v-model="selectedTeam" :options="teams" optionLabel="teamName" placeholder="팀을 선택하세요" @change="filterByDepartmentAndTeam" class="mr-2" />
                    </div>
                    <div class="relative search-container">
                        <i class="pi pi-search search-icon" />
                        <InputText v-model="filters['global'].value" placeholder="검색어를 입력해주세요" class="pl-8 search-input" />
                    </div>
                </div>
            </template>
            <template #empty> No employees found. </template>
            <Column field="deptName" sortable header="부 서" style="min-width: 12rem">
                <template #body="{ data }">
                    {{ data.deptName }}
                </template>
            </Column>
            <Column field="teamName" sortable header="팀" style="min-width: 12rem">
                <template #body="{ data }">
                    {{ data.teamName }}
                </template>
            </Column>
            <Column field="employeeName" sortable header="이 름" style="min-width: 12rem">
                <template #body="{ data }">
                    {{ data.employeeName }}
                </template>
            </Column>
            <Column field="jobRoleName" sortable header="직무" style="min-width: 12rem">
                <template #body="{ data }">
                    {{ data.jobRoleName }}
                </template>
            </Column>
            <Column field="positionName" sortable header="직 책" style="min-width: 12rem">
                <template #body="{ data }">
                    {{ data.positionName }}
                </template>
            </Column>
            <Column field="employeeId" sortable header="사 번" style="min-width: 12rem">
                <template #body="{ data }">
                    {{ data.employeeId }}
                </template>
            </Column>
            <Column field="joinDate" sortable header="입사일" dataType="date" style="min-width: 10rem">
                <template #body="{ data }">
                    {{ formatDate(new Date(data.joinDate)) }}
                </template>
            </Column>
        </DataTable>

        <EmployeeDetailModal :employee="selectedEmployee" :visible="displayDialog" @update:visible="displayDialog = $event" />
    </div>
</template>

<script setup>
import router from '@/router';
import EmployeeDetailModal from '@/views/pages/employeeDetail/EmployeeDetailModal.vue';
import { FilterMatchMode, FilterOperator } from '@primevue/core/api';
import { onBeforeMount, ref } from 'vue';
import { fetchGet } from '../auth/service/AuthApiService';

const employees = ref([]);
const filteredEmployees = ref([]);
const filters = ref(null);
const selectedEmployee = ref(null);
const displayDialog = ref(false);
const departments = ref([]);
const teams = ref([]);
const selectedDepartment = ref(null);
const selectedTeam = ref(null);

function handleDepartmentChange(event) {
    console.log('부서 변경됨:', selectedDepartment.value);

    // 팀 드롭다운 초기화
    selectedTeam.value = null; // 팀을 초기화하여 전체 팀으로 변경

    filterByDepartmentAndTeam();
}

async function fetchEmployeeList() {
    try {
        // 권한이 필요한 페이지에 접근할 때 fetchAuthorizedPage 사용
        const employeesData = await fetchGet('https://hq-heroes-api.com/api/v1/employee/active/employees', router.push, router.currentRoute.value);

        if (employeesData) {
            // JSON.parse 제거
            employees.value = employeesData;
        } else {
            employees.value = [];
        }

        filterByDepartmentAndTeam();
    } catch (error) {
        console.error('직원 데이터를 가져오는 중 오류 발생:', error);
        employees.value = [];
    }
}

async function fetchDepartments() {
    try {
        const departmentsData = await fetchGet('https://hq-heroes-api.com/api/v1/employee/departments', router.push, router.currentRoute.value);

        if (departmentsData) {
            // JSON.parse 제거
            departments.value = [{ deptId: null, deptName: '전체 부서' }, ...departmentsData];
        } else {
            departments.value = [{ deptId: null, deptName: '전체 부서' }];
        }
    } catch (error) {
        console.error('부서 데이터를 가져오는 중 오류 발생:', error);
        departments.value = [{ deptId: null, deptName: '전체 부서' }];
    }
}

async function fetchTeams(deptId) {
    try {
        const teamsData = await fetchGet(`https://hq-heroes-api.com/api/v1/employee/teams?deptId=${deptId}`, router.push, router.currentRoute.value);

        if (teamsData) {
            teams.value = [{ teamId: null, teamName: '전체 팀' }, ...teamsData]; // JSON.parse 제거
        } else {
            teams.value = [{ teamId: null, teamName: '전체 팀' }];
        }
    } catch (error) {
        console.error('팀 데이터를 가져오는 중 오류 발생:', error);
        teams.value = [{ teamId: null, teamName: '전체 팀' }];
    }
}

// 부서 및 팀에 따라 직원 목록 필터링
function filterByDepartmentAndTeam() {
    console.log('선택된 부서:', selectedDepartment.value); // 로그 추가
    console.log('부서 변경됨:', selectedDepartment.value); // 로그 추가

    if (selectedDepartment.value && selectedDepartment.value.deptId) {
        fetchTeams(selectedDepartment.value.deptId); // 선택한 부서 ID 전달
    } else {
        teams.value = [{ teamId: null, teamName: '전체 팀' }]; // 부서가 선택되지 않은 경우 기본 팀 추가
    }

    filteredEmployees.value = employees.value.filter((employee) => {
        const matchesDepartment = !selectedDepartment.value || selectedDepartment.value.deptName === '전체 부서' || employee.deptName === selectedDepartment.value.deptName;
        const matchesTeam = !selectedTeam.value || selectedTeam.value.teamName === '전체 팀' || employee.teamName === selectedTeam.value.teamName;
        return matchesDepartment && matchesTeam;
    });
}

// 필터 초기화
function initFilters() {
    filters.value = {
        global: { value: null, matchMode: FilterMatchMode.CONTAINS },
        employeeName: { operator: FilterOperator.AND, constraints: [{ value: null, matchMode: FilterMatchMode.STARTS_WITH }] },
        deptName: { operator: FilterOperator.AND, constraints: [{ value: null, matchMode: FilterMatchMode.EQUALS }] },
        jobRoleName: { operator: FilterOperator.AND, constraints: [{ value: null, matchMode: FilterMatchMode.EQUALS }] },
        teamName: { operator: FilterOperator.AND, constraints: [{ value: null, matchMode: FilterMatchMode.EQUALS }] },
        positionName: { operator: FilterOperator.AND, constraints: [{ value: null, matchMode: FilterMatchMode.EQUALS }] },
        employeeId: { operator: FilterOperator.AND, constraints: [{ value: null, matchMode: FilterMatchMode.EQUALS }] },
        joinDate: { operator: FilterOperator.AND, constraints: [{ value: null, matchMode: FilterMatchMode.DATE_IS }] }
    };
}

// 직원 상세 보기 함수
function showEmployeeDetails(event) {
    selectedEmployee.value = event.data;
    displayDialog.value = true;
}

// 날짜 포맷팅 함수
function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function onRowSelect(event) {
    console.log('선택된 직원:', event.data);
    selectedEmployee.value = event.data;
    displayDialog.value = true;
}

function onRowUnselect(event) {
    console.log('선택 해제된 직원:', event.data);
    selectedEmployee.value = null;
    displayDialog.value = false;
}

// 컴포넌트 마운트 시 데이터 가져오기
onBeforeMount(() => {
    fetchEmployeeList();
    fetchDepartments();
    initFilters();
});
</script>

<style scoped lang="scss">
:deep(.p-datatable-frozen-tbody) {
    font-weight: bold;
}

.search-container {
    display: flex;
    align-items: center;
    position: relative;
}

.search-icon {
    position: absolute;
    left: 10px;
    top: 50%;
    transform: translateY(-50%);
    color: #aaa;
}

.search-input {
    padding-left: 2.5rem;
}
</style>

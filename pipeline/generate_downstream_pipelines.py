import os
import requests

downstream_pipeline_exclusion_variable = 'EXCLUDE_FROM_TAKX_DOWNSTREAM_PIPELINE'
base_url = 'https://git.tak.gov/api/v4/'
pagination_suffix = '/?per_page=100&page=' # Max/page per the API docs is 100; requesting more still maxes out at 100
raptor_plugins_url = base_url + 'groups/2277/projects' + pagination_suffix
voltron_plugins_url = base_url + 'groups/2639/projects' + pagination_suffix

payload = {}
headers = {
    'PRIVATE-TOKEN': os.environ['GITLAB_ACCESS_TOKEN'],
}

def isPipelineDefined(project_id, branch):
    response = requests.get(base_url + 'projects/' + str(project_id) + '/repository/files/.gitlab-ci.yml?ref=' + branch,
                            headers=headers, data=payload)
    return response.status_code == 200

def isExcludedFromDownstreamPipeline(project_id):
    response = requests.get(base_url + 'projects/' + str(project_id) + '/variables/' + downstream_pipeline_exclusion_variable,
                            headers=headers, data=payload)
    return response.status_code == 200

def appendJobsToPipeline(url, headers, payload, base_path):
    nextPage = "1"

    while nextPage != "":
        full_url = url + nextPage
        response = requests.get(full_url, headers=headers, data=payload)

        if (response.status_code != 200):
            print('Failed to query for projects: ' + full_url)
            print(response.status_code)
            print(response.content)
            return

        for obj in response.json():
            if (obj['only_allow_merge_if_pipeline_succeeds'] and isPipelineDefined(obj['id'], obj['default_branch']) and
                    not isExcludedFromDownstreamPipeline(obj['id'])):
                path = str(obj['path_with_namespace']).replace(' ', '')

                dynamic_pipeline.write('job' + path.replace(base_path, '').replace('/', '_') + '_downstream:\n')
                dynamic_pipeline.write('  stage: deploy\n')
                dynamic_pipeline.write('  trigger: ' + path + '\n')
                dynamic_pipeline.write('  rules:\n')
                dynamic_pipeline.write('    - if: $CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH\n\n')

        nextPage = response.headers['X-Next-Page']

dynamic_pipeline = open('generated_downstream_pipelines.yml', 'w')

appendJobsToPipeline(raptor_plugins_url, headers, payload, 'raptor/takx-plugins')
appendJobsToPipeline(voltron_plugins_url, headers, payload, 'raptor/voltron')

dynamic_pipeline.close()
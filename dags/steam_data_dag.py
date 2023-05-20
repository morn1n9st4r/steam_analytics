from datetime import datetime
import requests
import ast

from billiard import Pool

from airflow import DAG

from airflow.operators.python_operator import PythonOperator
from airflow.operators.bash_operator import BashOperator

from airflow.hooks.S3_hook import S3Hook

AIRFLOW_DIR = "/opt/airflow/"

def get_data_from_api(url):
    response = requests.get(url)
    if "appdetails" in url:
        return [response.json()]
    else:
        return list(response.json().values())
        


def callApi(**kwargs):
    ti = kwargs['ti']

    urls_ids = str(kwargs['urls']) \
        .replace("[", "") \
        .replace("]", "") \
        .replace(", None", "") \
        .split(", ")
    header = kwargs['header']
    filename = kwargs['filename']

    urls = [header + str(id) for id in urls_ids]

    with Pool(processes=4) as pool:
        json_ans = []
        for data in pool.imap_unordered(get_data_from_api, urls):
            try:
                json_ans.extend(data)
                # keep track of uploading progress
                if (len(json_ans) % 10 == 0):
                    print(len(json_ans))
            except (IndexError, TypeError):
                continue
        pool.close()
        pool.join()

    file_path = f"{AIRFLOW_DIR}{filename}.json" 
    with open(file_path, "w") as file:
        file.write("[")
        for json_one in json_ans:
            file.write(str(json_one))
            file.write(",\n")
        file.write("]")


def upload_to_s3(connection_id, filename, key, bucket_name):
    hook = S3Hook(connection_id)
    hook.load_file(filename=filename,
                   key=key, 
                   bucket_name=bucket_name,
                   replace=True)

def get_dicts_from_file(filename):
    dicts = []
    file_path = f"{AIRFLOW_DIR}{filename}.json" 
    with open(file_path, "r") as file:
        lines = file.readlines()
        for line in lines:
            js = {}
            if line.startswith("["):
                js = ast.literal_eval(line[1:-2])
            elif line.startswith("{"):
                js = ast.literal_eval(line[0:-2])
            dicts.append(js)
    return dicts

def get_ids_from_dicts(**kwargs):
    filename = kwargs['filename']
    ti = kwargs['ti']

    dicts = get_dicts_from_file(filename)
    ids = []
    for js in dicts:
        ids.append(js.get("appid"))
        
    ti.xcom_push(key='ids of games', value=ids)

with DAG('process_steam_data_with_api',
        description='get data from steamspy api, store on S3 and process it to AWS Redshift',
        max_active_runs=1,
        start_date=datetime(2023, 3, 21),
        schedule_interval="@daily",
        catchup=False
    ) as dag:

        get_basic_app_info = PythonOperator(
            task_id=f'get_basic_app_info',
            python_callable=callApi,
            op_kwargs={
                'urls': "0",
                'header': 'https://steamspy.com/api.php?request=all&page=',
                'filename': 'steam_simple'
             }
        )

        upload_json_to_s3 = PythonOperator(
            task_id='upload_json_to_s3',
            python_callable=upload_to_s3,
            op_kwargs={
                'connection_id': "AWS",
                'filename': f'{AIRFLOW_DIR}steam_simple.json',
                'key': 'steam_simple.json',
                'bucket_name': 'steam-json-bucket'
            }
        )

        get_ids_from_json = PythonOperator(
            task_id='get_ids_from_json',
            python_callable=get_ids_from_dicts,
            op_kwargs={
                'filename': 'steam_simple'
            }
        )

        remove_json_locally = BashOperator(
            task_id="remove_json_locally",
            bash_command=f"rm {AIRFLOW_DIR}steam_simple.json",
        )

        get_full_app_info = PythonOperator(
            task_id='get_full_app_info',
            python_callable=callApi,
            op_kwargs={
                'urls': '{{task_instance.xcom_pull(key="ids of games", task_ids="get_ids_from_json")}}',
                'header': 'https://steamspy.com/api.php?request=appdetails&appid=',
                'filename': 'steam_full'
            }
        )

        get_basic_app_info >> upload_json_to_s3 >> get_ids_from_json >> remove_json_locally
        get_ids_from_json >> get_full_app_info
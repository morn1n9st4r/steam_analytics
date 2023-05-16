from datetime import datetime
import requests

from airflow import DAG

from airflow.operators.python_operator import PythonOperator
from airflow.operators.bash_operator import BashOperator

from airflow.hooks.S3_hook import S3Hook

AIRFLOW_DIR = "/opt/airflow/"

def callApi(**kwargs):
    ti = kwargs['ti']

    urls = kwargs['urls']
    filename = kwargs['filename']

    json_ans = []
    for url in urls:
        response = requests.get(url)
        if "appdetails" in url:
            game_details = response.json()
            json_ans.append(game_details)
        else:
            games_simple = list(response.json().values())
            json_ans.extend(games_simple)

    file_path = f"{AIRFLOW_DIR}{filename}.json" 
    with open(file_path, "w") as file:
        for json_one in json_ans:
            file.write(str(json_one))
            file.write(",\n")


def upload_to_s3(connection_id, filename, key, bucket_name):
    hook = S3Hook(connection_id)
    hook.load_file(filename=filename,
                   key=key, 
                   bucket_name=bucket_name,
                   replace=True)


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
                'urls': ['https://steamspy.com/api.php?request=all&page=0'],
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

        remove_json_locally = BashOperator(
            task_id="remove_json_locally",
            bash_command=f"rm {AIRFLOW_DIR}steam_simple.json",
        )

        get_basic_app_info >> upload_json_to_s3 >> remove_json_locally
        
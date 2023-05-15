from airflow import DAG

from airflow.operators.python_operator import PythonOperator
from airflow.operators.bash_operator import BashOperator
from airflow.providers.amazon.aws.operators.s3 import S3CreateObjectOperator

AIRFLOW_DIR = "/opt/airflow/"

def callApi(urls):
    json_ans = []
    for url in urls:
        response = requests.get(url)
        individual_objects = list(response.json().values())
        json_ans.extend(individual_objects)

    file_path = f"{AIRFLOW_DIR}steam_simple.json" 
    with open(file_path, "w") as file:
        for json_one in json_ans:
            file.write(str(json_one))
            file.write(",\n")


with DAG('process_steam_data_with_api',
         description='get data from steamspy api and process it to AWS Redshift',
         schedule="@weekly",
         default_args=args,
         max_active_runs=1,
         catchup=False
    ) as dag:

        get_basic_app_info = PythonOperator(
            task_id=f'get_basic_app_info_task',
            python_callable=callApi("https://steamspy.com/api.php?request=all&page=0")
        )

        upload_json_to_s3 = S3CreateObjectOperator(
            task_id="upload_json_to_s3_task",
            s3_key="s3://steam-json-bucket/steam_simple.json",
            data=f"{AIRFLOW_DIR}steam_simple.json"
        )
        
        remove_json_locally = BashOperator(
            task_id="remove_json_locally_task",
            bash_command=f"rm {AIRFLOW_DIR}steam_simple.json",
        )

        get_basic_app_info >> upload_json_to_s3 >> remove_json_locally
        
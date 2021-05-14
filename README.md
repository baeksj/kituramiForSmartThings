## kituramiForSmartThings
DTH for kiturami multiroom boiler in local http resverse proxy environment

This function is only tested by myself.

![image](https://user-images.githubusercontent.com/1823785/118262354-be6e1180-b4ef-11eb-9941-a6c5ac31cd41.png)


귀뚜라미 각방제어 시스템 보일러를 위한 SmartThings DTH 입니다.
귀뚜라미 KRB API 는 SmartThings Cloud 를 통한 연결이 허용되지 않고 있는 관계로
이 기능은 Local 환경의 Reverse proxy 를 경유하여 KRB API 에 접속 합니다.


## 1. Reverse proxy 설정 
요구사항: docker 가 상시 실행될 수 있는 서버

`/nginx_proxy/kiturami.conf` 파일을 통해 docker 환경의 nginx 서버를 구동

아래의 코드를 참고하여 환경을 구성하세요

`$ git clone https://github.com/baeksj/KituramiForSmartThings`

`$ docker run -d --name nginx_proxy --net host -v ./KituramiForSmartThings/nginx_proxy:/etc/nginx/conf.d:z nginx`

nginx proxy 는 아래 설정과 같이 `http://localhost:8989` 를 `https://igis.krb.co.kr` 로 Pass 합니다.
기본 포트 8989 는 변경해도 무방합니다.

<pre>
server {
        listen 8989;

        location / {
                proxy_ssl_server_name on;
                proxy_pass https://igis.krb.co.kr;
        }
}
</pre>

아래와 같이 표시되면 reverse proxy 구성은 성공 입니다.
<pre>
$ telnet localhost 8989
Trying 127.0.0.1...
Connected to localhost.
Escape character is '^]'.
^]
telnet> q
</pre>

## 2. DTH 설치
SmartThings IDE 에서 DTH 를 설치 합니다.
멀티룸 보일러는 메인컨트롤러에 해당되는 Master device 와 각방컨트롤러에 해당되는 Child device 로 나눠집니다.
두가지 DTH 를 모두 설치 합니다.

## 3. Device 추가
SmartThings IDE 에서 Device 를 직접 추가 합니다.
Device Handler 는 위에서 추가한 "Kiturami-multiroom-boiler" 를 선택 합니다.

![image](https://user-images.githubusercontent.com/1823785/118262385-c9c13d00-b4ef-11eb-9c5e-8fb4e174d5fb.png)



## 4. Device 환경 설정
스마트폼 SmartThings 앱을 통해 추가된 보일러 Device 를 확인합니다.
설정으로 이동하여 설정 값을 입력 합니다.
IP: Docker 가 실행되는 서버IP : Port ex> 192.168.0.100:8989
userId: 귀뚜라미 원격 제어를 위한 앱 로그인 ID 를 입력 합니다.
password: 귀뚜라미 원격 제어를 위한 앱 로그인 비밀번호 를 입력 합니다.
away/off 선택: SmmartThings 를 통한 switch off action 발생시 보일러를 전원종료 할지 외출모드 를 할지 선택 합니다.

![image](https://user-images.githubusercontent.com/1823785/118262404-d0e84b00-b4ef-11eb-850d-6ea6b337cac7.png)
![image](https://user-images.githubusercontent.com/1823785/118262414-d47bd200-b4ef-11eb-908c-c21a6a05ded6.png)

환경설정이 완료되면 메인컨트롤러에 연결된 각방컨트롤러가 Child device 로 자동 추가됩니다.

## 5. 사용
귀뚜라미 KRB API 는 연속 메시지가 다수 발생하면 일시적으로 동작이 멈추는 현상이 자주 발생합니다.
이를 막기위해서 매 Command 실행 후 결과 상태조회시 Alive 체크를 하는데 이 API 의 서버 response 가 1-2초 정도 걸립니다.
사용중 답답해도 어쩔수 없습니다.

## SmartThings 표준 Capability 중 Thermostat modes 는 '목욕' 이 없어서, Resume 을 선택하면 '목욕' 기능이 동작합니다.

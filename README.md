## kituramiForSmartThings
DTH for kiturami multiroom boiler in local http resverse proxy environment

## 주의사항
이 기능은 개인적으로 사용하기 위해 개발 한 내용으로, 동작의 정확성이 보장되지 않습니다.
본 기능의 문제 혹은 KRB API 의 문제로 인해 예상치 못한 보일러 동작으로 피해를 입을 수 있습니다.
이 기능의 사용으로 인한 결과적 책임은 모두 본인에게 있음을 인지하고 사용하시기 바랍니다.
귀뚜라미 IOT 각방제어 시스템이 설치된 환경에서만 동작 합니다.

![image](https://user-images.githubusercontent.com/1823785/118262354-be6e1180-b4ef-11eb-9941-a6c5ac31cd41.png)


귀뚜라미 각방제어 시스템 보일러를 위한 SmartThings DTH 입니다.

## 1. Reverse proxy 설정 (필수아님)

귀뚜라미의 SmartThings Cloud 서버로부터 KRB API 서버 접속이 불가능한 경우
Local 서버에 구성된 Reverse proxy 를 경유 접속 하는 기능 입니다.
설정에서 선택을 통해 KRB API 로 직접접속 Yes 선택 한 경우에는 필요 없는 설정 입니다.

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

![image](https://user-images.githubusercontent.com/1823785/118264270-68e73400-b4f2-11eb-8e9f-aef0108d5af6.png)



## 3. Device 추가
SmartThings IDE 에서 Device 를 직접 추가 합니다.
Device Handler 는 위에서 추가한 "Kiturami-multiroom-boiler" 를 선택 합니다.

![image](https://user-images.githubusercontent.com/1823785/118262385-c9c13d00-b4ef-11eb-9c5e-8fb4e174d5fb.png)



## 4. Device 환경 설정
스마트폰 SmartThings 앱을 통해 추가된 보일러 Device 를 확인합니다.
설정으로 이동하여 설정 값을 입력 합니다.

![image](https://user-images.githubusercontent.com/1823785/118262404-d0e84b00-b4ef-11eb-850d-6ea6b337cac7.png)
![image](https://user-images.githubusercontent.com/1823785/118349100-9206d380-b589-11eb-945f-b150f003eb2d.png)

- KRB API 직접접속 여부선택 : 기본 Yes, Yes 선택시 SmartThings Cloud 서버에서 KRB API 서버에 직접 접속합니다.
직접접속이 실패하는 경우 No 를 선택하고 아래의 Internal 서버 주소를 통해 Reverse proxy 경유 접속도 가능합니다.

- IP: 귀뚜라미 API 직접 접속을 하지 않는 경우에 필요한 설정 입니다. docker 가 실행되는 서버 IP

- id: 귀뚜라미 앱 로그인 ID

- password: 귀뚜라미 앱 로그인 패스워드

- off/away: 앱 device off 시 전원종료/외출모드 선택

환경설정이 완료되면 메인컨트롤러에 연결된 각방컨트롤러가 Child device 로 자동 추가됩니다.

## 5. 사용
귀뚜라미 KRB API 는 연속 메시지가 다수 발생하면 일시적으로 동작이 멈추는 현상이 자주 발생합니다.
이를 막기위해서 매 Command 실행 후 결과 상태조회시 Alive 체크를 하는데 이 API 의 서버 response 가 1-2초 정도 걸립니다.
사용중 답답해도 어쩔수 없습니다.

## SmartThings 표준 Capability 중 Thermostat modes 는 '목욕' 이 없어서, Resume 을 선택하면 '목욕' 기능이 동작합니다.

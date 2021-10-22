## kituramiForSmartThings
DTH for kiturami multiroom boiler

## 주의사항
이 기능은 개인적으로 사용하기 위해 개발 한 내용으로, 동작의 정확성이 보장되지 않습니다.
본 기능의 문제 혹은 KRB API 의 문제로 인해 예상치 못한 보일러 동작으로 피해를 입을 수 있습니다.
이 기능의 사용으로 인한 결과적 책임은 모두 본인에게 있음을 인지하고 사용하시기 바랍니다.
귀뚜라미 IOT 각방제어 시스템이 설치된 환경에서만 동작 합니다.

![image](https://user-images.githubusercontent.com/1823785/118262354-be6e1180-b4ef-11eb-9941-a6c5ac31cd41.png)


귀뚜라미 각방제어 시스템 보일러를 위한 SmartThings DTH 입니다.
단일제어 보일러에서 동작은 테스트 해 보지 않았습니다.


## 1. DTH 설치
SmartThings IDE 에서 DTH 를 설치 합니다.
멀티룸 보일러는 메인컨트롤러에 해당되는 Master device 와 각방컨트롤러에 해당되는 Child device 로 나눠집니다.
두가지 DTH 를 모두 설치 합니다.

![image](https://user-images.githubusercontent.com/1823785/118264270-68e73400-b4f2-11eb-8e9f-aef0108d5af6.png)



## 2. Device 추가
SmartThings IDE 에서 Device 를 직접 추가 합니다.
Device Handler 는 위에서 추가한 "Kiturami-multiroom-boiler" 를 선택 합니다.

![image](https://user-images.githubusercontent.com/1823785/118262385-c9c13d00-b4ef-11eb-9c5e-8fb4e174d5fb.png)



## 3. Device 환경 설정
스마트폰 SmartThings 앱을 통해 추가된 보일러 Device 를 확인합니다.
설정으로 이동하여 설정 값을 입력 합니다.

![image](https://user-images.githubusercontent.com/1823785/118262404-d0e84b00-b4ef-11eb-850d-6ea6b337cac7.png)
![image](https://user-images.githubusercontent.com/1823785/118349100-9206d380-b589-11eb-945f-b150f003eb2d.png)

- KRB API 직접접속 여부선택 : 기본 Yes 선택합니다.
  No 선택시 Internal 서버 주소를 통해 Reverse proxy 경유 접속도 가능합니다.
- IP: 귀뚜라미 API 직접 접속을 하지 않는 경우 Proxy 주소 입니다. (입력불필요)
- id: 귀뚜라미 앱 로그인 ID
- password: 귀뚜라미 앱 로그인 패스워드
- off/away: 앱 device off 시 전원종료/외출모드 선택

환경설정이 완료되면 메인컨트롤러에 연결된 각방컨트롤러가 Child device 로 자동 추가됩니다.

## 4. 사용
- 귀뚜라미 KRB API 는 연속 메시지가 다수 발생하면 일시적으로 동작이 멈추는 현상이 발생 할 수 있습니다.
- 가정내 제어기를 통해 제어한 결과는 SmartThing 에 최대 1분 내 반영 됩니다.
- 모드
  Away : 외출모드,
  난방/Heat : 난방모드,
  꺼짐/Off : 꺼짐,
  Resume : 목욕모드,
  Schedule : 예약모드

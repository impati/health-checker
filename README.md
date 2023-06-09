# health-checking

---

클라우드 환경에 서버를 배포하고 난 후 알 수 없는 이유로 왕왕 서버가 내려가 있었다.

이를 해결하기위한 방법으로는 
- 직접 접속하여 커맨드를 통해 작동하고 있는지 확인한다.
- 클라우드 서비스 중 헬스체크 기능을 사용한다.
- 직접 서비스에 접속하여 확인한다.
- 직접 헬스체크 서버를  구현한다.


직접 서버 헬스 체크 기능을 구현하여 서버가 다운시 사용자의 이메일로 알림을 보내어 

이 문제를 해결해보자

# 첫 번째 해결 방법

--- 

개요 : 동기 호출 , 로컬 캐싱 , 큐 초기화를 통한 동기화

- 활성화 서버를 로컬 우선순위 큐로 삽입합니다.
- **1 분마다** 우선순위 큐를 검사하여 헬스 체크를 수행합니다.
- 헬스 체크를 수행은 동기적으로 내부 HealthCheckManager 를 호출하여 수행합니다.
- 서버 비활성화 , 새로운 활성화 서버 등록 등 DB 변화가 생긴 후 큐를 동기화 하는 작업은 **30분마다** 기존의 큐를 비우고 새로운 큐를 생성합니다.   


## 기능



### Check Queue


하나의 쓰레드가 **1분마다** 우선순위 큐를 체크합니다.

- 큐에 있는 대상 서버가 현재 시간과 동일한 경우 헬스체크를 실시합니다.
- 헬스 체크를 실시하기전에 큐에서 꺼내어 서버가 설정한 헬스 체크 간격 만큼 시간을 업데이트한 후 큐에 삽입합니다.


### Synchronize Queue

- **30분** 마다 검사하는 클래스를 재호출해주며 동기화를 수행해줍니다.
- 기존의 큐를 비우고 새로운 호출로 DB 로부터 활성화된 서버를 새롭게 큐로 삽입합니다. 


### health Check

- 약속된 서버에 요청을 보내고 정상 응답인지를 확인합니다.
- 정상 응답이라면 정상응답을 저장하고 종료합니다.
- 정상 응답이 아니라면 3번 연속시도 후 그럼에도 실패시 메일로 알람을 보내고 실패응답을 저장하고 종료합니다.


### Send Mail
- Server Checking 실패 후 사용자 이메일로 실패 이메일을 보낸다.
- 연속 두번 실패시 더이상 보내지 않으며 비활성화 상태로 전환합니다.

## 장점

- 로컬 큐를 사용함으로써 헬스 체크 서버 대상에 빠르게 접근할 수 있습니다.

## 단점

- 로컬 큐를 사용하기 때문에 헬스 체크 서버 대상이 늘어날 경우 메모리 부족으로 이어질 수 있습니다.
- 헬스 체크 기능을 동기적으로 수행하기 때문에 헬스 체크 서버 대상이 늘어날 경우 요청이 지연될 수 있습니다.
- 동기화 과정이 필요없는 경우에도 동기화를 수행합니다.


---

# 클래스 관계

<img src="docs/class.png" width="700">

- HealthTimeChecker : 1분 마다 큐를 검사할 HealthTargetChecker 를 비동기 호출한다.
- HealthTargetImporter: 활성화된 서버  정보를 가져오는 역할을 가집니다.
- HealthTargetChecker : 큐에 있는 헬스 체크 대상 서버를 체크하고  HealthCheckRequester 를 호출하는 역할을 가집니다.
- HealthCheckRequester : 대상 서버에 헬스 기능을 호출하는 역할을 가집니다.
- HealthCheckManager : 헬스 체크를 호출하고 성공과 실패 Manager 를 호출하는 역할을 가집니다.
- HealthChecker : 헬스 체크하는 역할을 담당합니다.
- HealthCheckSuccessManager : 헬스 체크가 성공했을 때 성공 레코드를 저장하는 역할을 가집니다.
- HealthCheckFailManager : 헬스 체크가 실패했을 때 실패 레코드를 저장하고 알람을 보내는 역할을 가집니다.


# 프로세스

---
## check queue , Synchronize queue

<img src="docs/sync.png" width="700">

1. HealthTimeChecker 가 1분마다 HealthTargetChecker 를 비동기 호출하여 검사에 지연이 발생하지 않도록합니다. 
2. HealthTargetChecker 는 HealthTargetImporter 로 부터 서버 정보를 담고 있는 큐를 초기화하거나 동기화합니다. 
3. HealthTargetImporter 는 활성화된 서버를 가져옵니다. 
4. HealthTargetChecker 는 서버 정보를 담고 있는 큐를 확인하여 체크 대상 서버에 대해 HealthCheckRequester 호출하여 헬스 체크를 수행해줍니다. 
5. HealthCheckRequester 는 동기적으로 헬스 체크를 수행합니다.

## health check , sendMail

<img src="docs/check-sequence.png" width="700">

- 성공 시
1. HealthCheckManager 는 HeathChecker 를 호출하여 서버에 실제 호출을 수행합니다.
2. 헬스 체크 성공 시 성공 레코드를 저장합니다.

- 실패 시
1. HealthCheckManager 는 HeathChecker 를 호출하여 서버에 실제 호출을 수행합니다.
2. 헬스 체크 살패 시 성공 레코드를 저장하고 종료합니다.
3. 실패 알람을 서버를 등록한 사용자 이메일로 보냅니다.



# 두 번째 방법

---
첫 번째 방법의 HealthCheckRequester 를 동기에서 비동기로 구현하는 것입니다.

## 첫 번째 방법의 문제점

--- 
첫 번째 방법에서 한 번에 체크해야 할 대상 서버가 N 개일 경우 모두 동기 호출을 진행하기 때문에 M1 MAC PRO 기준으로 N = 10_000일 때 평균 9.1초,
N = 100_000일 때 98초가 걸렸습니다.

로컬 캐싱을 사용하면서 1분 단위로 검사를 진행하기 때문에 검사 시간이 60초를 넘어가는 경우 데이터 정합성의 문제가 발생하여
헬스 체크를 받아야 할 서버가 영원히 받지 못하는 starvation 상황이 발생할 수 있다.


## 비동기로 전환

--- 
HealthCheckRequester 가 호출하는 HealthCheckManager 의 경우 반환타입이 void 입니다.

즉 HealthCheckRequester 가 호출하고나서 응답을 받고 활용하지 않기 때문에 동기로 동작할 필요가 없습니다.

HealthCheckManager 을 비동기로 호출하는 AsyncInnerHealthCheckRequester 를 구현하고  HealthCheckRequester 구현체를
SynchronousInnerHealCheckRequester 에서 AsyncInnerHealthCheckRequester 로 변경합니다.

<img src="docs/async.png" width="700">


## 개선점 

---

비동기 호출로 전환만으로 N = 10_000일 때 평균 9.1초에서 2.8초로 단축, N = 100_000일 때 98초에서 29초로 단축했다.
(여기서 N은 로컬 큐에 들어있는 활성화된 서버 수)
## 남은 문제점

---

여전히 로컬 캐싱을 사용하기 때문에 메모리 문제와 마지막으로 저장한 HealthRecord 를 가져와 동기화를 진행하는 작업에서 N 값이 (활성화된 서버 수 와 저장된 HealthRecord 수) 커질수록
쿼리가 1분을 넘어가는 상황이 발생.



# 세 번째 방법

---

두 번째 방법에서 로컬 큐를 사용하지 않고 데이터베이스에 ActiveServer 를 관리하는 Table 를 두어 현재 시간에 헬스 체크를 수행해야할 서버만 가져오는 방법이다. \
헬스 체크를 수행하고 난 후 혹은 서버의 활성화 / 비활성화 시 그때마다 ActiveServer Table 을 수정함으로써 별도의 동기화 작업을 진행하지 않는 방법이다.

## 활성화 Table 을 별도로 관리

---
Active Table 은 활성화된 서버 , 즉 헬스 체크 대상 서버 정보만을 관리하는 데이터베이스 Table 이다.

현재 활성화된 서버만 저장되며 비활성화로 전환시 삭제된다.

Active Table 에는 다음에 수행할 헬스 체크 시간을 저장하여 관리한다.

- HealthTargetChecker 의 구현체인 LocalQueueHealthTargetChecker 를
    ActiveTable 에서 헬스 체크를 수행해야하는 서버 정보를 가져와 HealthCheckRequester 를 호출하는  ActiveTableHealthTargetChecker 로 변경한다.

- HealthTargetImporter 의 구현체인 DefaultHealthTargetImporter 를
  ActiveTable 에서 헬스 체크를 수행해야하는 서버 정보를 가져오는 ActiveTableHealthTargetImporter 로 변경한다.



<img src="docs/active.png" width="700">

<img src="docs/table.png" width="700">




## 개선점

---
두 번째 방법은 (비동기 호출 + 동기화 시간)이 총 걸리는 시간이 었지만 세 번째 방법은 비동기 호출만이 걸리는 총 시간이다. \
다만 트랜잭션에서 ActiveServer Table 을 동기화하는 로직 및 시간이 추가된다. \
또한 현재 시간에 헬스 체크를 수행해야할 서버 정보만을 가져오기 때문에 메모리 문제에서 벗어날 수 있다.


## 개선할 수 있는 점

---

- RestTemplate 는 동기 , blocking 방식으로 동작한다. WebClient 도입으로 개선
- Server 정보를 조회해오는 것을 캐싱



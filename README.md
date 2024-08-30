# Оркестратор

Необходимо реализовать individuals api (оркестратор), который будет отвечать за взаимодействие с внешним миром и оркестрацию вызовов “внутренних” сервисов.

## Функционал:
- Регистрация и логин пользователей
- Получение данных по пользователю
- Данные о пользователе хранятся в KeyCloak (данные для аутентификации).\
## Technology stack:
- Java 21
- Spring WebFlux
- Spring Security
- KeyCloak
- TestContainers
- Junit 5
- Mockito
- Docker
### Пример cURL запроса на регистрацию
```plaintext 
curl -X 'POST' \
'https://HOST/v1/auth/registration' \
-H 'accept: */*' \
-H 'Content-Type: application/json' \
-d '{
"email": "string",
"password": "string",
"confirm_password"
}' 
```


### Пример тела ответа:

```plaintext 
{
"access_token": "string",
"expires_in": 0,
"refresh_token": "string",
"token_type": "string"
}
```
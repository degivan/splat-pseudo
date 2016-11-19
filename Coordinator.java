class Coordinator {
	start() { //запуск
		createOurExecService(); //создаем наш пул
		ourExecService.addTask(new RecoveryTask()); //восстанавливаем предыдущее состояние ExecServic'а в случае падения
		ourExecService.addTask(new ClientMessagesReaderTask());
		ourExecService.addTask(new ClientMessagesWriterTask());
		ourExecService.addTask(new QueueTask()); 
	}
}

/**
 * Наш ExecutorService. Поддерживает инвариант: thread(rollbackTransaction) == thread(billingTransaction)
 */
class ourExecService {
	addBillingTask(BillingTask billingTask) {
		transaction = billingTask.transaction;
		id = getUniqueTransactionId(transaction);
		thread = getThreadForId(id);
		thread.addTask(billingTask);
	}

	addRollbackTask(RollbackTask rollbackTask) {
		transaction = billingTask.transaction;
		id = getUniqueTransactionId(transaction);
		thread = getThreadForId(id);
		thread.addTask(rollbackTask);
	}

	addPunterTask(...)

	addEventTask()

	addBetTask(...)


}

/**
 * Восстанавливаемся после падения
 */
class RecoveryTask {
	run() {
		//поднять логи
		//восстановить незаконченные задачи на тот момент
	}
}

/**
 * Запускает первую и вторую фазу, контролирует, чтобы между запуском и концом первой фазы прошло < 10 секунд, иначе делает откат
 */
class QueueTask {
	run() {
		//создаем для всех реквестов, которые лежат в нашей глобальной очереди и еще не обработаны, FirstPhaseTask
		//Дальше смотрим для всех тасок, которые в очереди: если прошло 10 секунд, а ответ по FirstPhaseTask еще не получен, запускаем RollbackTask и пишем негативный ответ пользователю
		//Если пришел положительный ответ по FirstPhaseTask, отправляем ответ пользователю, а сами запускаем вторую фазу(SecondPhaseTask)
	}
}

/**
 * Отправляет запросы сервисам с определенной частотой; если пришли все, объединяет результаты
 */
class FirstPhaseTask {
	Transaction transaction;

	run() {
		loop { //c определенной частотой
			Future<Boolean> bllingResult = ourExecService.addTask(new BillingTask());
			Future<Boolean> eventResult = ourExecService.addTask(new EventTask());
			..
			if(allResultsAreReady)
				return billingResult.get() && eventResult.get() && ...;
		}
	}
}

/**
 * Отправляем запросы второй фазы, пока все не пройдут
 */
class SecondPhaseTask {
	Transaction transaction;

	run() {
		loop { //c определенной частотой, пока не получили ответы от всех сервисов
			//отправляем запросы на снятие денег; изменение лимитов; подтверждение ставки
		}
	}
}

/**
 * В отдельном процессе принимаем заявки от клиентов
 */
class ClientMessagesReaderTask {
	void run() {
		while(true) {
			checkForUnfinishedActions(); //могли не успеть передать заявки на обработку перед тем как упасть, это надо проверять
			requests = readNewRequests(); //считываем еще непрочитанные заявки
			requests.addToGlobalQueue(); //ставим их все на обработку 
		}
	}
}

/**
 * В отдельном процессе отдаем ответы клиентам
 */
class ClientMessagesWriterTask {
	void run() {
		while(true) {
			checkForUnfinishedActions(); //могли не успеть передать ответы перед тем как упасть, это надо проверять
			answers = readNewAnswers(); //считываем новые ответы
			answers.forEach(writeAnswer(answer, answer.client)); //отправляем их все
		}
	}
}		
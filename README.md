# Умный замок на Android Things и Raspberry Pi3


## Введение

В декабре 2016 года Google [анонсировал](https://android-developers.googleblog.com/2016/12/announcing-googles-new-internet-of-things-platform-with-weave-and-android-things.html) выход первой Developer Preview версии Android Things. С тех пор проект сильно изменился. Все еще доступна только preview-версия, но с каждым шагом у платформы появляются новые возможности и растет число поддерживаемых устройств. 

С каждым днем появляются новые примеры использования IoT устройств в реальном мире, а сама платформа становится все более привлекательной. Мы в [Live Typing](https://livetyping.com/ru/) решили тоже погрузиться в интереснейший мир Интернет Вещей и рассказать о своем опыте. Эта статья для тех, кто слышал об Android Things, но боялся попробовать. А также о том, как мы реализовали свой «умный замок» и пользуемся им в собственном офисе. 
## Описание идеи

Проблема №1: Наша компания снимает офис с системой электронных пропусков и стеклянными дверями. Часто сотрудники забывают свои карточки дома или просто выходят на улицу без них, а потом стучатся или звонят коллегам, чтобы попасть обратно. Карточку нужно прикладывать к магнитному замку и внутри, и снаружи офиса. Если внутри мы просто привязали карточку на веревочку, то попасть в офис без ключа снаружи — это проблема, которую мы бы хотели решить. 

Проблема №2: По выходным в нашем офисе проводятся разного рода митапы. Основная часть присутствующих не является нашими коллегами. Их количество варьируется, но сколько бы их ни было, дать ключ постороннему человеку мы не можем, как и держать дверь все время открытой — это небезопасно для нашего имущества. Поэтому сейчас приходится назначать специального «человека-швейцара» или подпирать дверь чем придётся. 

Отключить, убрать или модернизировать проходную систему мы не имеем права, но на то, чтобы подключить что-то снаружи, ограничения не распространяются. Мы решили оборудовать дверь сервоприводом, который будет поворачивать прикреплённую карточку к считывающему датчику при успешном распознавании лица. Фотографию лица предоставляет камера. Таким образом получаем этакий умный дверной замок. 

С таким замком у нас появляется своя идентификационная система с блэкджеком и широкими возможностями. (Например смешные фотографий сотрудников, из которых можно делать стикеры для внутреннего шутливого использования). Если говорить о второй проблеме, то замок позволяет сделать регистрацию и пропуск по фотографиям участников встречи. Awesome, не правда ли?

Дальше идея обрастала сопутствующими нюансами и вопросами. Когда начинать и заканчивать фотографировать? Как часто и долго делать фотографии? Стоит отключать систему в нерабочие часы и темное время суток? Как визуализировать работу системы? Но обо всем этом далее и по отдельности. За основу проекта мы взяли пример [Doorbell](https://developer.android.com/things/training/doorbell/index.html) с официальной страницы Android Things. Оригинальный пример называется «звонок», однако мы хотели, чтобы пользователь системы отпирал дверь с минимальными усилиями, а посторонние люди внутрь не попали. Поэтому мы посчитали более правильным назвать его «умный замок». 
## Благодарности

Сначала у нас не было ничего. Ни самого Raspberry, ни комплектующих, ни опыта работы с ними — только теоретические знания, полученные из статей и документации. Первый раз попробовать поиграться c Android Things удалось на [CodeLab](https://github.com/Mobilatorium/smart-doorbell-codelab) проводимом в нашей IT-столице Сибири, городе Омск, ребятами из [Mobilatorium](http://mobilatorium.org/). Мы быстро завели проект, где вместо Google Cloud Vision имплементировали свою реализацию FindFace на [Tensor Flow](https://www.tensorflow.org). Если вам интересно, как устроен back-end, то можете ознакомиться с отличной статьей [«Ищем знакомые лица»](https://habrahabr.ru/post/317798/), где автор очень подробно описал принципы и алгоритмы работы с распознаванием лиц. Если нет, то можете воспользоваться связкой [Google Cloud Vision](https://cloud.google.com/vision/) + [Firebase Realtime Database](https://firebase.google.com/docs/database/), как это сделано в приведённом выше CodeLab.

Когда мы вернулись в офис, оказалось что у нашего сотрудника Миши есть все необходимые компоненты и даже сам Raspberry Pi3, которые он недавно приобрёл, желая побаловаться чем-то эдаким. Также остались компоненты от ребят, проводивших [летнюю школу] (https://vk.com/mobilatorium?w=wall-130802553_81%2Fall) с изучением Arduino. Огромное им спасибо за предоставленные железяки.
## Комплектующие

Для реализации умного замка нам понадобились:

- Raspberry Pi 3 — 1шт;
- MicroSD 8Gb — 1шт;
- NoIR Camera V2 — 1шт;
- Breadboard — 1шт;
- Infrared PIR Motion Sensor Module — 1шт;
- SG90 Servo Motor — 1шт;
- Photoresistor (Light Sensor) — 1шт;
- Push Button — 1шт;
- LED — 3 шт;
- Resistors (1k) — 3шт;
- Resistors (10k) — 1 шт;
- Pin Jumper Wires — много штук;
- Изолента — 1шт.

Все комплектующие легко найти и недорого заказать на китайских сайтах, и мы специально оставили их названия на английском для удобства поиска. Стоит лишь упомянуть, что комплект обойдётся вам примерно в 100-125$. Самыми дорогими компонентами являются камера и сам Raspberry Pi3. 
## Реализация

Для лучшего понимания мы разобьём описание реализации на отдельные шаги. Соединяя схему по частям, удобней восстановить картину на любом шаге. Кода мало, и если вы написали хотя бы одно приложение под Android, то проблем у вас не возникнет, на мой взгляд. Для разработки будем использовать привычную Android Studio. Вы даже сможете использовать свои любимые библиотеки и фреймворки, такие как Dagger, RxJava, Retrofit, OkHttp, Timber и т.п. 

Перед началом работ стоит ознакомиться с кратким введением в [Android Things](https://habrahabr.ru/post/318296/), а также с [пинами на Raspberry Pi3](https://developer.android.com/things/images/pinout-raspberrypi.png). А эта цветная картинка с распиновкой является отличным наглядным гайдом и пригодится вам ещё не один раз.


Raspberry Pi поддерживает разный набор интерфейсов оборудования. Но нас главным образом интересуют [GPIO](https://developer.android.com/things/sdk/pio/gpio.html) (General-purpose input/output) и [PWM](https://developer.android.com/things/sdk/pio/pwm.html) (Pulse Width Modulation). Они будут основными способами взаимодействия между платой и датчиками при реализации нашего проекта. 

Библиотеки для различных периферийных устройств уже написаны до нас, а многие из них доступны даже сразу из коробки. Поэтому, когда начнёте интегрировать новый датчик, сначала ознакомтесь с [этим](https://github.com/amitshekhariitbhu/awesome-android-things#drivers)  и [этим](https://github.com/androidthings/contrib-drivers) репозиториями. Здесь собрано множество драйверов. Скорее всего, вы найдете нужный. Если нет, то Google предоставили специальный концепт [User Drivers](https://developer.android.com/things/sdk/drivers/index.html), который расширяет возможности [Android Framework Services](https://anatomyofandroid.com/2013/10/12/android-framework/). Он перенаправляет происходящие в железе во фреймворк и позволяет обработать их стандартными средствами Android API и таким образом создать свой драйвер. Коротко работу с любым драйвером можно разбить на следующие этапы:
- создать объект драйвера;
- зарегистрировать драйвер;
- подписаться на события;
- отписаться и отменить регистрацию;
Можете ознакомиться с примером реализации собственного драйвера в [этой статье](https://www.novoda.com/blog/writing-your-first-android-things-driver-p1/).

### Шаг 1: Установка Android Things

Устанавливаем самый свежий [образ Android Things](https://developer.android.com/things/hardware/raspberrypi.html#flashing_the_image) на Raspberry Pi3. Там же приведены ссылки на инструкции по установке образа для различных операционных систем. 

Убедиться, что всё успешно установлено, можно, подключив к Raspberry какой-нибудь дисплей через HDMI-кабель. Если все окей, то вы увидите на экране анимацию загрузки Android Things. 

![img](http://cdn01.androidauthority.net/wp-content/uploads/2017/01/android-things-booting-in-tv-16x9-720p-840x472.jpg)



Для более комфортного взаимодействия с устройством в документации советуют [настроить подключение по WiFi](https://developer.android.com/things/hardware/raspberrypi.html#connecting_wi-fi). После этого внизу экрана под заставкой Android Things появится IP-адрес девайса в вашей WiFi сети. 

![img](https://androidthings.rocks/images/android-things-ethernet.png)



Если в вашей сети только одно такое устройство, то можно не запоминать адрес и не проверять его всякий раз при изменении, а воспользоваться зарезервированным Raspberry именем хоста и подключаться через adb командой.


```java
$ adb connect Android.local
```

### Шаг 2: Создание приложения

Создаём новое приложение через Android Studio. Можно визуализировать работу своей программы стандартными Android виджетами, разместив их экране, хотя это не обязательно. Ознакомьтесь с [полной инструкцией создания первого Android Things приложения](https://developer.android.com/things/training/first-device/create-studio-project.html) на официальном сайте, а мы разберём только основные моменты.

Минимальные требования: 
- Android Studio 2.2 и выше;
- SDK Tools 25.0.3 и выше;
- Min SDK 24 и выше;
- Target SDK 24 и выше.

Добавим в `app/build.gradle` зависимость Android Things support library, которая даст нам доступ к нужному API, не являющемся частью стандартного Android SDK.

```java
dependencies {
    ...
    provided 'com.google.android.things:androidthings:0.4-devpreview'
    ...
}
```


Каждое приложение связывается с используемой по умолчанию библиотекой Android, в которой имеются базовые пакеты для построения приложений (со стандартными классами, например Activity, Service, Intent, View, Button, Application, ContentProvider и так далее).
Однако некоторые пакеты находятся в собственных библиотеках. Если ваше приложение использует код из одного из таких пакетов, оно должно в явном виде потребовать, чтобы его связали с этим пакетом. Это делается через отдельный элемент <uses-library>. 

```xml
<application ...>
    ...
    <uses-library android:name="com.google.android.things"/>
    ...
</application>
```

Android Things позволяет одновременно устанавливать только одно приложение, а больше нам и не надо. Благодаря этому ограничению появляется возможность декларировать `<intent-filter>` для Activity, как `IOT_LAUCHER` в AndroidManifest приложения, что позволяет запускать это Activity по-умолчанию сразу же при старте девайса. Также оставим стандартный `<intent-filter>` чтобы Android Studio смогла запустить наше приложение после сборки и деплоя.

```xml
<activity ...>
    ...
    <!-- Launch activity as default from Android Studio -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.LAUNCHER"/>
    </intent-filter>
     
    <!-- Launch activity automatically on boot -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.IOT_LAUNCHER"/>
        <category android:name="android.intent.category.DEFAULT"/>
    </intent-filter>
    ...
</activity>
```

### Шаг 3: Кнопка

Начнём с подключения тактовой кнопки, при нажатии на которую камера сделает один снимок. Это простой механизм: толкатель нажимается — цепь замыкается. Кнопка с четырьмя контактами представляет собой две пары соединительных рельс. При замыкании и размыкании между пластинами кнопки возникают микроискры, провоцирующие многократные переключения за крайне малый промежуток времени. Такое явление называется дребезгом. Подробней о [кнопке](https://goo.gl/6SymSw).

![raspberry_step#1](https://monosnap.com/file/hasVSud1ExjRLG0Uy2KfDLNQMralP0.png) 

Кнопка подключается через макетную плату с использованием резистора на 1 кОм. Чтобы не запутаться в резисторах обратите, внимание на их [цветовую кодировку](https://goo.gl/TlsUYf). Не будем подробно расписывать процесс подключения. Просто сопоставьте представленную схему с распиновкой Raspberry, данной чуть выше. 

Для интеграции кнопки используем [готовый драйвер](https://github.com/androidthings/contrib-drivers/tree/master/button), который уже учитывает эффект дребезга. Добавим зависимость


```java
dependencies {
    ...
    compile 'com.google.android.things.contrib:driver-button:0.3'
    ...
}
```

Напишем класс-обёртку для работы с кнопкой. Возможно, реализация через обёртку покажется немного излишней, но таких образом мы сможем инкапсулировать работу с кодом драйвера кнопки и создать собственный интерфейс взаимодействия.

```java
import com.google.android.things.contrib.driver.button.Button;
  
public class ButtonWrapper {
 
   private @Nullable Button mButton;
 
   private @Nullable OnButtonClickListener mOnButtonClickListener;
 
   public ButtonWrapper(final String gpioPin) {
       try {
           mButton = new Button(gpioPin, Button.LogicState.PRESSED_WHEN_HIGH); 
           mButton.setOnButtonEventListener(new Button.OnButtonEventListener() {
               @Override
               public void onButtonEvent(Button button, boolean pressed) {
                   if (pressed && mOnButtonClickListener != null) {
                      mOnButtonClickListener.onClick();
                   }
               }
           });
 
       } catch (IOException e) {
           e.printStackTrace();
       }
   }
 
   public void setOnButtonClickListener(@Nullable final OnButtonClickListener listener) {
       mOnButtonClickListener = listener;
   }
 
   public void onDestroy() {
       if (mButton == null) {
           return;
       }
       try {
           mButton.close();
       } catch (IOException e) {
           e.printStackTrace();
       } finally {
           mButton = null;
       }
   }
 
   public interface OnButtonClickListener {
       public void onClick();
   }
}
```

Воспользуемся этой обёрткой в нашем Activity. Просто передадим в конструктор объекта кнопки название GPIO порта ("BCM4") на Raspberry Pi3, к которому она подключена на схеме. 


```java
public class MainActivity extends Activity {

    private static final String GPIO_PIN_BUTTON = "BCM4";

    private ButtonWrapper mButtonWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ...
        mButtonWrapper = new ButtonWrapper(GPIO_PIN_BUTTON);
        mButtonWrapper.setOnButtonClickListener(new ButtonWrapper.OnButtonClickListener() {
            @Override
            public void onClick() {
                Timber.d("BUTTON WAS CLICKED");
                startTakingImage();
            }
        });
        ...
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ...
        mButtonWrapper.onDestroy();
        ...
    }

    private void startTakingImage() {
        // TODO take photo
        ...
    }
}
```

### Шаг 4: Камера

Мы использовали камеру [NoIR Camera V2](https://www.raspberrypi.org/products/pi-noir-camera-v2/). Примерно за 45$ вы получите характеристики, достаточные для нашего проекта: 


- максимальное разрешение: 8 Мп (3280×2464);
- поддерживаемые видеоформаты: 1080p (30fps), 720p (60fps), 640×480p (90fps);
- энергопотребление: 250 мА

Камера подключается к управляющей плате шлейфом через видеовход CSI (Camera Serial Interface). Такой способ снижает нагрузку на центральный процессор по сравнению с подключением аналогичных камер по USB. 

Добавляем в манифест разрешение на использование камеры и требования, что устройство должно ею обладать. Добавление разрешений обязательно, однако согласие на все Permissions, в том числе и на Dangerous Permissions, получается автоматически при установке приложения (существует известная проблема, что при добавлении нового пермишена после переустановки приложения требуется полностью перезагрузить устройство).

```xml
    <uses-permission android:name="android.permission.CAMERA" />
    
    <uses-feature android:name="android.hardware.camera" />
    <uses-feature android:name="android.hardware.camera.autofocus" />
```

Для дальнейшего подключения камеры требуется написать большие по меркам этой статьи куски кода. Пожалуйста, ознакомьтесь с ними самостоятельно по [этой ссылке](https://developer.android.com/things/training/doorbell/camera-input.html) на официальном сайте. В коде устанавливаются настройки камеры. Мы не стали использовать всю её мощь и выбрали разрешение 480х320.

### Шаг 5: Светодиод

Светодиод — вид диода, который светится, когда через него проходит ток. Его собственное сопротивление после насыщения очень мало. При подключении потребуется резистор, который будет ограничивать ток, проходящий через светодиод, иначе последний просто перегорит. Подробней о [светодиодах](https://goo.gl/GmjwMl)

Мы будем использовать три светодиода разных цветов:
- жёлтый (`BCM20`) — индикатор начала работы, которая продолжается указанный разработчиком  интервал времени;
- зелёный (`BCM21`) — программа успешно распознала лицо из базы данных;
- красный (`BCM16`) — программа не распознала лицо в течении указанного интервала времени

Можно использовать один трехцветный светодиод вместо трёх одноцветных. Мы пользовались тем, что было под рукой. Используя резисторы в 1 кОм, поочередно подключаем наши светодиоды через макетную плату в соответствии с приведенной схемой. 

![raspberry_step#2](https://monosnap.com/file/aVjGeFIjTqUQ2W97bUpqgsihE9Y6D3.png)


При реализации обёртки для работы со светодиодом воспользуемся `PeripheralManagerService`, сервисом, который дает доступ к GPIO интерфейсу. Открываем соединение и конфигурируем его для передачи сигнала. К сожалению, если заглянуть в реализацию абстрактного класса `com.google.android.things.pio.Gpio`, то можно увидеть, что вызов почти каждого метода способен генерировать `java.io.IOException`. Для простоты скроем все `try-catch` выражения в нашей обертке. 


```java
public class LedWrapper {
 
    private @Nullable Gpio mGpio;
 
    public LedWrapper(String gpioPin) {
        try {
            PeripheralManagerService service = new PeripheralManagerService();
            mGpio = service.openGpio(gpioPin);
            mGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    public void turnOn() {
        if (mGpio == null) {
            return;
        }
        try {
            mGpio.setValue(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    public void turnOff() {
        if (mGpio == null) {
            return;
        }
        try {
            mGpio.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    public void onDestroy() {
        try {
            mGpio.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mGpio = null;
        }
    }
}
```

Имплементируем его в наше Activity для каждого светодиода по отдельности. 

```java
public class MainActivity extends Activity {


    private final static String GPIO_PIN_LED_GREEN = “BCM21”;
    private LedWrapper mLedWrapper;

    @Override   
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ...
        mLedWrapper = new LedWrapper(GPIO_PIN_LED_GREEN);      
        mLedWrapper.turnOff();
        ...
    }

    private void turnOn() {
        mLedWrapper.turnOn();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ...
        mLedWrapper.onDestroy();
        ...
    }
}
```
### Шаг 6: Датчик движения

Каждый раз подходить к двери и жать на кнопку, словно это дверной звонок, скучно. Мы хотели избавить гостя нашего офиса от лишних действий или даже вовсе открывать дверь, пока человек только подходит к двери. Поэтому мы решили использовать датчик движения как основной триггер для начала работы всей системы. На всякий случай оставим кнопку с дублирующей функциональностью как есть. Подробней о [датчике движения](https://learn.adafruit.com/pir-passive-infrared-proximity-motion-sensor?view=all).

Подключаем датчик движения через `BCM6` пин по схеме, приведенной ниже. 

![img_motion](https://monosnap.com/file/6NwCcv0SVY6d30VTu3EKQ3sKca7UZ7.png)


```java
public class MotionWrapper {

    private @Nullable Gpio mGpio;
    private @Nullable MotionEventListener mMotionEventListener;

    public MotionWrapper(String gpioPin) {
        try {
            mGpio = new PeripheralManagerService().openGpio(gpioPin);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMotionEventListener(@Nullable final MotionEventListener listener) {
        mMotionEventListener = listener;
    }

    public void startup() {
        try {
            mGpio.setDirection(Gpio.DIRECTION_IN);
            mGpio.setActiveType(Gpio.ACTIVE_HIGH);
            mGpio.setEdgeTriggerType(Gpio.EDGE_RISING);
            mGpio.registerGpioCallback(mCallback);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void shutdown() {
        if (mGpio == null) {
            return;
        }
        try {
            mGpio.unregisterGpioCallback(mCallback);
            mGpio.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void onDestroy() {
        try {
            mGpio.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mGpio = null;
        }
    }

    private final GpioCallback mCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            if (mMotionEventListener != null) {
                mMotionEventListener.onMovement();
            }
            return true;
        }
    };

    public interface MotionEventListener {
        void onMovement();
    }
}
```

### Шаг 7: Сервопривод

Сервопривод совершает основную механическую работу в нашем девайсе. Именно он поворотом выходного вала на 180 градусов подносит карточку к считывающему устройству. Подробней про [сервоприводы](https://goo.gl/djxOOz).

Мы снова используем уже [существующий драйвер](https://github.com/androidthings/contrib-drivers/tree/master/pwmservo), над которым напишем свою обёртку. Добавляем в `app/build.gradle` зависимость. 

```java
dependencies {
    ...
    compile 'com.google.android.things.contrib:driver-pwmservo:0.2'
    ...
}
```

Подключим привод через [интерфейс широтно-импульсной модуляции](https://goo.gl/2WiaR5) `PWM1` в соответствии со схемой приведенной ниже. Использование `PWM`интерфейса обусловлено тем, что, в отличии от предыдущих случаев, требуется передать конкретное значение через управляющий сигнал, а не просто бинарный импульс. Управляющий сигнал — импульсы постоянной частоты и переменной ширины. Сервопривод использует широтно-импульсный входящий PWM-сигнал, преобразуя его в конкретный угол поворота выходного вала.

![img_servo](https://monosnap.com/file/Ilby67uLNTie3owuee9DDd7BjnGxk7.png)


```java
public class ServoWrapper {

    private static final float ANGLE_CLOSE = 0f;
    private static final float ANGLE_OPEN = 180f;

    private Servo mServo;
    private Handler mHandler = new Handler();

    public ServoWrapper(final String gpioPin) {
        try {
            mServo = new Servo(gpioPin);
            mServo.setAngleRange(ANGLE_CLOSE, ANGLE_OPEN);
            mServo.setEnabled(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void open(final long delayMillis) {
        try {
            mServo.setAngle(ANGLE_OPEN);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mHandler.removeCallbacks(mMoveServoRunnable);
        if (delayMillis > 0) {
            mHandler.postDelayed(mMoveServoRunnable, delayMillis);
        }
    }

    public void close() {
        if (mServo == null) {
            return;
        }

        try {
            mServo.setAngle(ANGLE_CLOSE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onDestroy() {
        mHandler.removeCallbacks(mMoveServoRunnable);
        mMoveServoRunnable = null;

        if (mServo != null) {
            try {
                mServo.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mServo = null;
            }
        }
    }

    private Runnable mMoveServoRunnable = new Runnable() {
        @Override
        public void run() {
            mHandler.removeCallbacks(this);
            close();
        }
    };
}
```

### Шаг 8: Фоторезистор

В тёмное время суток работа замка бессмысленна, потому что по полученным фотографиям сложнее распознать лица. Значит, систему можно временно отключать. Для этого в качестве датчика света используем фоторезистор — Light Dependent Resistors (LDR). Подробней о [фоторезисторе](http://www.resistorguide.com/photoresistor/).

Для работы с фоторезистором подходит драйвер кнопки, описанный ранее. Это логично, ведь суть и механика работы действительно совпадают. В `app/build.gradle` уже должна быть подключена библиотека.

```java
dependencies {
    ...
    compile 'com.google.android.things.contrib:driver-button:0.3'
    ...
}
```

Схема подключения к макетной плате аналогична схеме подключения кнопки. Отличие лишь в использовании резистор в 10 кОм. Используем порт `BCM25`.

![doorbell_full_scheme_version](https://monosnap.com/file/mwJWD2hqHmrIitj3H3vSl2WOtUy0Z9.png)

Несмотря на всю похожесть, напишем для него отдельную обёртку. 


```java
public class BrightrWrapper {

    private @Nullable Button mLightDetector;

    private @Nullable OnLightStateChangeListener mOnLightStateChangeListener;

    public BrightrWrapper(final String gpioPin) {
        try {
            mLightDetector = new Button(gpioPin, Button.LogicState.PRESSED_WHEN_HIGH);
            mLightDetector.setOnButtonEventListener(new Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(Button button, boolean isLighted) {
                    if (mOnLightStateChangeListener != null) {
                        mOnLightStateChangeListener.onLightStateChange(isLighted);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setOnLightStateListener(@Nullable final OnLightStateChangeListener listener) {
        mOnLightStateChangeListener = listener;
    }

    public void onDestroy() {
        if (mLightDetector == null) {
            return;
        }
        try {
            mLightDetector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface OnLightStateChangeListener {
        public void onLightStateChange(boolean isLighted);
    }
}
```

Имплементируем в Activity.

```java
public class MainActivity extends Activity {

    private static final String GPIO_PIN_LIGHT_DETECTOR = "BCM25";

    private BrightrWrapper mBrightrWrapper;

    private boolean mIsTakePhotoAllowed = true;

    @Override   
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ...
        mBrightrWrapper = new BrightrWrapper(GPIO_PIN_LIGHT_DETECTOR);
        mBrightrWrapper.setOnLightStateListener(new BrightrWrapper.OnLightStateChangeListener() {
            @Override
            public void onLightStateChange(final boolean isLighted) {
                mIsTakePhotoAllowed = isLighted;
                handleLightState();
            }
        });
        ...
    }

    private void handleLightState() {
        if (mIsTakePhotoAllowed) {
            ...
        } else {
            ...
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ...
        mBrightrWrapper.onDestroy();
        ...
    }
}
```

## Демонстрация реализации

Пересмотрев все выпуски телепередачи «Очумелые ручки», мы искусно упаковали нашего «монстра» в коробочку из-под старого телефона. В разобранном виде это выглядит так

![img](https://monosnap.com/file/83zgUOAoG7u7cSzuPaW0lHJ2R5F02Z.png)

Ознакомиться с видео-демонстрацией вы можете по этой [ссылке](https://www.youtube.com/watch?v=7S_EBAqtHpI)

## Проблемы

Плохое качество датчиков и сенсоров. Срабатывают или не срабатывают, когда надо и когда не надо. Сервопривод потрескивает в режиме ожидания. 
Если вы ознакомились с принципом работы датчика движения, то могли понять, что через стекло он не срабатывает. Поэтому пришлось выводить его наружу. 
Разместив нашу конструкцию изнутри на тройной стеклопакет все фотографии оказались сильно засвечены. Быстро сообразив, что проблема заключается в отражении света от белой поверхности коробочки и его многократном преломлении через стеклопакет, мы просто наклеили чёрный лист бумаги вокруг камеры, чтобы он поглощал часть света. 
Нет защиты от распечатанных фотографий.

## Итоги

Пример кода на [Github](https://github.com/LiveTyping/door-unlocker-android). 

Интересно, захватывающе, модно, молодежно. Это был интересный проект, и идеи для его развития еще есть. Например, изнутри дверь также открывается по карточке. Можно решить и эту проблему — открывать дверь каждому, кто подходит к ней изнутри. Пожалуйста, предложите в комментариях свои варианты доработки нашего умного замка. Если они нам понравятся, то мы обязательно их реализуем. 


Какие варианты монетизации разработок под IoT знаете вы? Делитесь своим опытом в комментариях. 

Не на правах рекламы хочу поделиться отличной статьей, где автор придумал и реализовал самодельную читалку новостей шрифтом Брайля [BrailleBox](https://medium.com/exploring-android/braillebox-building-a-braille-news-reader-with-android-things-f848fe6de596) для слабо видящих на Android Things. Выглядит и реализовано так же круто, как и звучит. Отличный, воодушевляющий проект.
 
## Полезные ссылки

- [Android Things](https://developer.android.com/things/sdk/index.html)
- [Raspberry PI3](https://developer.android.com/things/hardware/raspberrypi.html)
- [Drivers #1](https://github.com/androidthings/contrib-drivers)
- [Drivers #2](https://github.com/amitshekhariitbhu/awesome-android-things#drivers)
- [Mobilatorium](https://github.com/Mobilatorium/smart-doorbell-codelab)
- [Building a Cloud Doorbell](https://developer.android.com/things/training/doorbell/index.html)
- [Sensor Motion #1](http://blog.blundellapps.co.uk/tut-android-things-writing-a-pir-motion-sensor-driver/)
- [Sensor Motion #2](https://github.com/blundell/PirMotionSensorModuleTut)
- [How sensor motion works](https://learn.adafruit.com/pir-passive-infrared-proximity-motion-sensor?view=all)
- [Servo](https://github.com/androidthings/drivers-samples/tree/master/pwmservo)
- [LED](https://androidthings.rocks/2017/01/08/your-first-blinking-led/)
- [Photoresistor](http://www.resistorguide.com/photoresistor/)
- [Photoresistor Sample](https://github.com/Polidea/at_candle)
- [Ultrasonic sensor](https://hackernoon.com/android-things-basics-measure-distance-with-ultrasonic-sensor-3196fe5d7d7c)
- [Rasberry3 Pins](https://www.raspberrypi.org/documentation/usage/gpio-plus-and-raspi2/)
- [About GPIO pins on Raspberry Pi](https://www.raspberrypi.org/documentation/usage/gpio-plus-and-raspi2/)
- [About GPIO pins](https://developer.android.com/things/sdk/pio/gpio.html)
- [About PWM pins](https://developer.android.com/things/sdk/pio/pwm.html)
- [Writing your first Android Things driver](https://www.novoda.com/blog/writing-your-first-android-things-driver-p1/). 

# Rest API

Поменяйте адрес `ENDPOINT` на ваш. 

### 1. Добавление фотографии

```
$ curl <ENDPOINT>/api/faces -X POST \
  -F "file=@IMG_0460.JPG" \
  -F 'group_id=test_group' \
  -F 'person_id=test_person' \
  -F 'image_id=test_image.jpg'
```

По внегласной догороренности мы приняли следующие правила именования 

`group_id` - идентификатор группы. Для сотрудников нашей компании = `ltst`

`person_id` - идентификатор человека. Для сотрудников нашей компании представляет собой фамилию, замисанную 
латиницей. Например `ivanov`.

`image_id` - название файла фотографии. Поскольку для одного человека на сервер может быть загружено 
одновременно несколько фотографий, то к заголовку имени файла `img.jpg` добавляем порядковый номер фотографии. Например 
`img1.jpg`. 


Response 200:

```
{
    "group_id":"test_group",
    "person_id":"test_person",
    "image_id":"test_image",
    "added_at":"2017-03-18T00:09:55.418Z"
}
```

Error:

```
{
    "error":{
        "id":"7692",
        "message":"Error: No faces found on image"
    }
}
```

### 2. Список всех фотографий

```
$ curl <ENDPOINT>/api/faces
```

Response:

```
[{
    "group_id":"test_group",
    "person_id":"test_person",
    "image_id":"test_image",
    "added_at":"2017-03-18T00:09:55.418Z"
},
{
    "group_id":"test_group",
    "person_id":"test_person_2",
    "image_id":"test_image",
    "added_at":"2017-03-18T00:10:12.012Z"
},
...
]
```

### 3. Удаление фотографии

```
$ curl <ENDPOINT>/api/faces/delete \
  -X POST -H "Content-Type: application/json" -d '{ \
  "group_id":"test_group", \
  "person_id":"test_person_2", \
  "image_id":"test_image" \
  }'
```

Response:

```
{
    "success":true
}
```


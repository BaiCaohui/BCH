# 闅愮涓庡畨鍏ㄥ姞鍥哄疄鏂借鍒?
> **For agentic workers:** REQUIRED SUB-SKILL: 鐢变富浠ｇ悊鎸?TDD 鍦ㄦ湰浼氳瘽鍐呴€愪换鍔℃墽琛岋紱姝ラ鐢?`- [x]` 璺熻釜銆?
**Goal:** 涓?BCH 鍔犲叆鍙厤缃殑鍙嶈拷韪€佺珯鐐规潈闄愩€丠TTPS 寮哄埗銆丆ookie/绔欑偣鏁版嵁绠＄悊銆侀闄╅槻鎶や笌骞垮憡鎷︽埅澧炲己銆?
**Architecture:** 绾€昏緫鏀?`browser/`锛圝VM 鍗曟祴瑕嗙洊锛夛紝鏁版嵁灞傛墿灞?Room/DataStore锛孶I 璧扮幇鏈夎缃〉涓庡脊绐楁ā寮忋€?
**Tech Stack:** Kotlin 2.0.20 路 Jetpack Compose/Material3 路 Room 路 DataStore 路 绯荤粺 WebView銆?
---

## 鏂囦欢缁撴瀯

- 鏂板缓锛歚browser/TrackerBlocker.kt`銆乣browser/HttpsPolicy.kt`銆乣browser/SitePermissionPolicy.kt`銆?  `browser/NotificationPolicy.kt`銆乣browser/DownloadRiskPolicy.kt`銆乣browser/CookieDataManager.kt`銆?  `ui/settings/PrivacySettingsScreens.kt`锛堝箍鍛婃嫤鎴?绔欑偣鏁版嵁锛夈€乣res/raw/trackers.txt`
- 淇敼锛歚data/prefs/BrowserPrefs.kt`銆乣data/db/Entities.kt`銆乣data/db/Daos.kt`銆乣data/db/AppDatabase.kt`銆?  `data/repo/SiteSettingsRepository.kt`銆乣browser/AdBlocker.kt`銆乣browser/WebClientPolicy.kt`銆?  `browser/BrowserWebView.kt`銆乣browser/WebViewStore.kt`銆乣browser/SiteSettingsPolicy.kt`銆?  `BchApp.kt`銆乣ui/browser/BrowserScreen.kt`銆乣ui/settings/OtherSettingsScreens.kt`銆?  `ui/settings/SettingsScreen.kt`銆乣ui/navigation/BchRoute.kt`銆乣AndroidManifest.xml`銆乣res/values/strings.xml`

## 浠诲姟 1锛氭暟鎹眰鍩虹锛圥refs / Entity / DB / Repository锛?
- [x] **姝ラ 1**锛欱rowserPrefs 鏂板瀛楁涓庡父閲?`HttpsMode`锛沗prefsVersion=6` 杩佺Щ銆?- [x] **姝ラ 2**锛歋iteSettingEntity 鏂板 9 涓彲绌哄垪锛汚ppDatabase v5 + `MIGRATION_4_5`锛汥AO 澧炲姞鎸変富鏈哄垹闃呰缂撳瓨銆?- [x] **姝ラ 3**锛歋iteSettingsRepository.upsert 鎵╁睍鍙傛暟锛汼iteSettingsPolicy.resolve 鎵╁睍銆?- [x] **姝ラ 4**锛氬啓/璺戝崟娴嬶紙榛樿鍊笺€佽縼绉讳笉涓㈡棫鍊笺€乺esolve 鍚堝苟锛夛紝鍏ㄧ豢鍚庣户缁€?
## 浠诲姟 2锛氬弽杩借釜 + 骞垮憡鎷︽埅澧炲己

- [x] **姝ラ 1**锛氬厛鍐?`TrackerBlockerTest`锛堜富鏈?瀛愬煙/闈?http 涓嶈浼わ級涓?`AdBlockerCustomRulesTest`锛坄||`銆佸煙鍚嶃€佸瓙涓层€乣*`锛夛紝璺戠孩銆?- [x] **姝ラ 2**锛氬疄鐜?`TrackerBlocker` + `trackers.txt`锛沗AdBlocker`/`CustomAdRules` 鏀寔鑷畾涔夎鍒欍€?- [x] **姝ラ 3**锛歚SiteSettingsPolicy` 瑙ｆ瀽 `antiTracking`锛沗BchWebViewClient` 娉ㄥ叆瑙勫垯涓庤窡韪櫒鍒ゅ畾锛堜粎瀛愯祫婧愶級銆?- [x] **姝ラ 4**锛氳窇娴嬭瘯杞豢銆?
## 浠诲姟 3锛欻TTPS 寮哄埗

- [x] **姝ラ 1**锛氬厛鍐?`HttpsPolicyTest`锛圤FF/PREFER/STRICT 鍗囩骇銆佸洖閫€銆侀檷绾у垽鏂級锛岃窇绾€?- [x] **姝ラ 2**锛氬疄鐜?`HttpsPolicy`锛沗BrowserWebView` 璁板綍鈥滃崌绾т腑鈥漊RL 闃插惊鐜紱`onPageStarted` 鍗囩骇銆?  `onReceivedError` PREFER 鍥為€€銆丼TRICT 鍥炶皟闃诲浜嬩欢锛涘湴鍧€鏍?`ensureLoaded` 鍏ュ彛鍗囩骇銆?- [x] **姝ラ 3**锛欱rowserScreen 椤堕儴涓嶅畨鍏ㄦí骞?+ STRICT 闃诲瀵硅瘽妗嗭紱绔欑偣绾?`httpsUpgrade` 鍚堝苟銆?- [x] **姝ラ 4**锛氳窇娴嬭瘯杞豢 + 缂栬瘧銆?
## 浠诲姟 4锛氱珯鐐规潈闄愮簿缁嗘帶鍒?
- [x] **姝ラ 1**锛氬厛鍐?`SitePermissionPolicyTest` 涓?`NotificationPolicyTest`锛堝叏灞€榛樿銆佺珯鐐硅鐩栥€佷富寮€鍏炽€佽祫婧愭槧灏勶級锛岃窇绾€?- [x] **姝ラ 2**锛氬疄鐜扮瓥鐣ワ紱WebCallbacks 澧炲姞 `onGeolocationPrompt`/`onPopup`/`onHttpsBlocked`锛?  WebChromeClient 鎺?`onGeolocationPermissionsShowPrompt`/`onCreateWindow`銆?- [x] **姝ラ 3**锛欱rowserScreen锛氭潈闄愮瓥鐣ラ┍鍔紙ALLOW 鐩存巿/ASK 寮圭獥/BLOCK 鎷掔粷锛夈€佷綅缃笌寮圭獥瀵硅瘽妗嗐€?  鑷姩鎾斁璁剧疆銆侀€氱煡娉ㄥ叆锛汳anifest 澧炲姞 4 涓潈闄愩€?- [x] **姝ラ 4**锛氳窇娴嬭瘯杞豢 + 缂栬瘧銆?
## 浠诲姟 5锛氶闄╅槻鎶わ紙涓嬭浇璀﹀憡锛?
- [x] **姝ラ 1**锛氬厛鍐?`DownloadRiskPolicyTest`锛堥珮鍗辨墿灞曞悕/MIME銆佹櫘閫氭枃浠朵綆鍗憋級锛岃窇绾€?- [x] **姝ラ 2**锛氬疄鐜扮瓥鐣ワ紱涓嬭浇纭妗嗘寜椋庨櫓鏄剧ず璀﹀憡鏂囨涓庡己璋冭壊銆?- [x] **姝ラ 3**锛氳窇娴嬭瘯杞豢銆?
## 浠诲姟 6锛欳ookie 涓庣珯鐐规暟鎹鐞?
- [x] **姝ラ 1**锛氬厛鍐?`CookieDataManagerTest`锛堝悕绉拌В鏋愩€佽繃鏈熼敭鍊笺€乭ttp/https 鍙屽啓锛夛紝璺戠孩銆?- [x] **姝ラ 2**锛氬疄鐜?`CookieDataManager`锛沗onPageStarted` 璁板綍涓绘満锛沗BchApp.onCreate` 鎵ц鍏抽棴鍗虫竻闄?  锛堝叏灞€ removeAllCookies + 閫愮珯鐐硅繃鏈熷垹闄わ級銆?- [x] **姝ラ 3**锛氭柊澧?`SiteDataScreen`锛氫富鏈哄垪琛ㄣ€丆ookie 鏁伴噺/鍚嶇О銆佸垹 Cookie/鍒犲叏閮ㄦ暟鎹?鍗曠珯鐐瑰叧闂嵆娓呴櫎銆?- [x] **姝ラ 4**锛氳窇娴嬭瘯杞豢 + 缂栬瘧銆?
## 浠诲姟 7锛氳缃?UI 涓庡瓧绗︿覆

- [x] **姝ラ 1**锛歅rivacyScreen 鏂板鍏ㄩ儴寮€鍏?妯″紡锛汼iteSettingsScreen 缂栬緫瀵硅瘽妗嗘柊澧?9 椤癸紱鏂板鈥滃箍鍛婃嫤鎴€濃€滅珯鐐规暟鎹€濊矾鐢变笌鍏ュ彛銆?- [x] **姝ラ 2**锛歴trings.xml 琛ラ綈鏂囨锛涚紪璇戦€氳繃銆?
## 浠诲姟 8锛氬叏閲忛獙璇?
- [x] **姝ラ 1**锛氬叏閲忓崟娴嬶紙`GRADLE_USER_HOME=D:\gradle-home`锛夊叏缁裤€?- [x] **姝ラ 2**锛欴ebug + Release 鏋勫缓鎴愬姛銆?- [x] **姝ラ 3**锛氭ā鎷熷櫒楠岃瘉娓呭崟锛氬弽杩借釜鎷︽埅銆佺珯鐐规潈闄愩€丠TTPS 妯箙/鍗囩骇銆佺珯鐐规暟鎹鐞嗐€佸嵄闄╀笅杞芥彁绀恒€佽嚜瀹氫箟瑙勫垯銆佸悇寮€鍏冲叧闂敓鏁堛€?- [x] **姝ラ 4**锛氭姤鍛婁笌閬楃暀杈圭晫銆?

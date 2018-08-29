# Current Version 
[![](https://jitpack.io/v/FrankChoo/FRecyclerView.svg)](https://jitpack.io/#FrankChoo/FRecyclerView)

# How to integration
### Step 1
Add it in your **root build.gradle** at the end of repositories
```
allprojects {
    repositories {
	...
	maven { url 'https://jitpack.io' }
    }
}
```

### Step 2
Add it in your **module build.gradle** at the end of repositories
```
dependencies {
    ...
    implementation "com.github.FrankChoo:FRecyclerView:$CurrentVersion"
    implementation 'com.android.support:appcompat-v7:27.+'
    implementation 'com.android.support:design:27.+'
    implementation 'com.android.support:recyclerview-v7:27.+'
}
```

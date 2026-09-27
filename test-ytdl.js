const ytdl = require('@distube/ytdl-core');
ytdl.getInfo('LUgpPmj6nR8').then(info => {
  const format = ytdl.chooseFormat(info.formats, { filter: 'audioonly' });
  console.log(format.url);
}).catch(console.error);

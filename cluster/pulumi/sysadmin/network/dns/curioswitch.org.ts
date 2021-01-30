/*
 * MIT License
 *
 * Copyright (c) 2021 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import * as aws from '@pulumi/aws';

const zone = new aws.route53.Zone('curioswitch.org', {
  name: 'curioswitch.org',
});

const a = (subdomain: string, targets: string[]) =>
  new aws.route53.Record(
    `${subdomain}.curioswitch.org`,
    {
      zoneId: zone.zoneId,
      name: `${subdomain}.curioswitch.org`,
      type: 'A',
      ttl: 300,
      records: targets,
    },
    {
      parent: zone,
    },
  );

const cname = (subdomain: string, target: string) =>
  new aws.route53.Record(
    `${subdomain}.curioswitch.org`,
    {
      zoneId: zone.zoneId,
      name: `${subdomain}.curioswitch.org`,
      type: 'CNAME',
      ttl: 300,
      records: [target],
    },
    {
      parent: zone,
    },
  );

const txt = (subdomain: string, targets: string[]) =>
  new aws.route53.Record(
    `${subdomain}.curioswitch.org:txt`,
    {
      zoneId: zone.zoneId,
      name: `${subdomain}.curioswitch.org`,
      type: 'TXT',
      ttl: 300,
      records: targets,
    },
    {
      parent: zone,
    },
  );

const rootTxt = new aws.route53.Record(
  'curioswitch.org:txt',
  {
    zoneId: zone.zoneId,
    name: 'curioswitch.org',
    type: 'TXT',
    ttl: 300,
    records: [
      'google-site-verification=F1MEXE9dJl8B8ggcYK8-cD23Cnl70LyrGzzfUyqjYpg',
      'v=spf1 include:_spf.google.com ?all',
    ],
  },
  {
    parent: zone,
  },
);

const rootMx = new aws.route53.Record(
  'curioswitch.org:mx',
  {
    zoneId: zone.zoneId,
    name: 'curioswitch.org',
    type: 'MX',
    ttl: 3600,
    records: [
      '1 aspmx.l.google.com',
      '5 alt1.aspmx.l.google.com',
      '5 alt2.aspmx.l.google.com',
      '10 alt3.aspmx.l.google.com',
      '10 alt4.aspmx.l.google.com',
    ],
  },
  {
    parent: zone,
  },
);

const developers = a('developers', ['151.101.1.195', '151.101.65.195']);

const emailMailgun = cname('email.mailgun', 'mailgun.org');

const googleDomainKey = new aws.route53.Record(
  'google._domainkey.curioswitch.org:txt',
  {
    zoneId: zone.zoneId,
    name: 'google._domainkey.curioswitch.org',
    type: 'TXT',
    ttl: 3600,
    records: [
      'v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCe13ONTkQz3NoTITiTasjfnvTjLRPEK2ltwRW2CrPHqV3J0q0l0Hi7XcDxZ1dHw5ZZaNbv8M3VRJv7hrR2kO/QIQwqbsmNyd0wXoRmauQRp/sJpJBb4aGqCXe/4DplsfsuIAG1UEMIigL/7X0dRnae/ZJfywsEs37bzOacDI1OqwIDAQAB',
    ],
  },
  {
    parent: zone,
  },
);

const mailgunDomainKey = txt('krs._domainkey.mailgun', [
  'k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC6r1WGaE6Sz9UuWKxIizxvn0NLk6KRDU6SCnQzu1PbgxG80UekkWz+UWC7xblf4K8wv15hFO96UZKAj4wBIa5luMxvgIGcVUDwyzGsWwJ6HWdnZafTDwJLbaXNVis7j9Zdi9RWAzuU+NWVha2GqK/I/P9B8gn+XRdryfysAMoPXQIDAQAB',
]);

const mailgunTxt = txt('mailgun', ['v=spf1 include:mailgun.org ~all']);

const mailgunMx = new aws.route53.Record(
  'mailgun.curioswitch.org:mx',
  {
    zoneId: zone.zoneId,
    name: 'mailgun.curioswitch.org',
    type: 'MX',
    ttl: 300,
    records: ['10 mxa.mailgun.org', '10 mxb.mailgun.org'],
  },
  {
    parent: zone,
  },
);

const s = cname('s', 's.curioswitch.org.s3.amazonaws.com');

const www = cname(
  'www',
  'curiobalancer-1597516419.ap-northeast-1.elb.amazonaws.com',
);

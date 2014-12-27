
<table cellpadding="0" cellspacing="3">
      <tr>
         <th>From</th>
         <th>Count</th>
         <th>Size</th>
         <th>% Read</th>
      </tr>
      {{#counts}}
      <tr style="background-color:{{backgroundColor}}">
         <td>{{from}}</td>
         <td>{{count}}</td>
         <td>{{size}}</td>
         <td>{{percentRead}}</td>
      </tr>
      {{/counts}}
</table>
